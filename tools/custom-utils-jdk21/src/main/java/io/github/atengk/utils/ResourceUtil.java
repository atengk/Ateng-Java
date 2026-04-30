package io.github.atengk.utils;

import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.WritableResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.w3c.dom.Document;

/**
 * 基于 Spring Resource 的资源工具类。
 *
 * @author Ateng
 * @since 2026-04-30
 */
public final class ResourceUtil {

    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
    private static final String CLASSPATH_PREFIX = ResourceLoader.CLASSPATH_URL_PREFIX;
    private static final String CLASSPATH_ALL_PREFIX = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX;
    private static final String FILE_PREFIX = "file:";
    private static final String DEFAULT_BINARY_CONTENT_TYPE = "application/octet-stream";
    private static final Pattern PROTOCOL_PATTERN = Pattern.compile("^([a-zA-Z][a-zA-Z0-9+.-]*):.*$");
    private static final Pattern TEMPLATE_VARIABLE_PATTERN = Pattern.compile("\\$\\{([^}]+)}");
    private static final Set<String> TEXT_EXTENSIONS = Set.of("txt", "md", "csv", "json", "xml", "yaml", "yml", "properties", "sql", "html", "htm", "css", "js", "ts", "java", "log");
    private static final Map<String, byte[]> RESOURCE_CACHE = new ConcurrentHashMap<>();

    private ResourceUtil() {
        throw new UnsupportedOperationException("ResourceUtil 不允许实例化");
    }

    /**
     * 根据资源定位字符串创建资源。
     *
     * @param location 资源定位字符串
     * @return 资源对象
     */
    public static Resource of(String location) {
        return load(location);
    }

    /**
     * 根据资源定位字符串和类加载器创建资源。
     *
     * @param location    资源定位字符串
     * @param classLoader 类加载器
     * @return 资源对象
     */
    public static Resource of(String location, ClassLoader classLoader) {
        requireText(location, "资源定位字符串不能为空");
        Objects.requireNonNull(classLoader, "类加载器不能为空");
        return new DefaultResourceLoader(classLoader).getResource(location);
    }

    /**
     * 创建 classpath 资源。
     *
     * @param path classpath 路径
     * @return classpath 资源
     */
    public static Resource classpath(String path) {
        requireText(path, "classpath 路径不能为空");
        return new ClassPathResource(removeClasspathPrefix(path));
    }

    /**
     * 根据文件路径创建资源。
     *
     * @param path 文件路径
     * @return 文件系统资源
     */
    public static Resource file(String path) {
        requireText(path, "文件路径不能为空");
        return new FileSystemResource(path);
    }

    /**
     * 根据 Path 创建资源。
     *
     * @param path 文件路径
     * @return Path 资源
     */
    public static Resource file(Path path) {
        Objects.requireNonNull(path, "文件路径不能为空");
        return new PathResource(path);
    }

    /**
     * 根据 File 创建资源。
     *
     * @param file 文件对象
     * @return 文件系统资源
     */
    public static Resource file(File file) {
        Objects.requireNonNull(file, "文件对象不能为空");
        return new FileSystemResource(file);
    }

    /**
     * 根据 URL 字符串创建资源。
     *
     * @param url URL 字符串
     * @return URL 资源
     */
    public static Resource url(String url) {
        requireText(url, "URL 不能为空");
        try {
            return new UrlResource(url);
        } catch (MalformedURLException ex) {
            throw new ResourceOperationException("URL 格式错误: " + url, ex);
        }
    }

    /**
     * 根据 URI 创建资源。
     *
     * @param uri URI 对象
     * @return URL 资源
     */
    public static Resource uri(URI uri) {
        Objects.requireNonNull(uri, "URI 不能为空");
        try {
            return new UrlResource(uri);
        } catch (MalformedURLException ex) {
            throw new ResourceOperationException("URI 无法转换为资源: " + uri, ex);
        }
    }

    /**
     * 根据字节数组创建资源。
     *
     * @param bytes 字节数组
     * @return 字节数组资源
     */
    public static Resource bytes(byte[] bytes) {
        Objects.requireNonNull(bytes, "字节数组不能为空");
        return new NamedByteArrayResource(Arrays.copyOf(bytes, bytes.length), null, "Byte array resource");
    }

    /**
     * 根据字节数组和文件名创建资源。
     *
     * @param bytes    字节数组
     * @param filename 文件名
     * @return 字节数组资源
     */
    public static Resource bytes(byte[] bytes, String filename) {
        Objects.requireNonNull(bytes, "字节数组不能为空");
        requireText(filename, "文件名不能为空");
        return new NamedByteArrayResource(Arrays.copyOf(bytes, bytes.length), filename, "Byte array resource: " + filename);
    }

    /**
     * 根据字符串内容创建资源。
     *
     * @param content 字符串内容
     * @param charset 字符集
     * @return 字节数组资源
     */
    public static Resource string(String content, Charset charset) {
        Objects.requireNonNull(content, "字符串内容不能为空");
        Objects.requireNonNull(charset, "字符集不能为空");
        return bytes(content.getBytes(charset));
    }

    /**
     * 根据输入流创建一次性资源。
     *
     * @param inputStream 输入流
     * @return 输入流资源
     */
    public static Resource stream(InputStream inputStream) {
        Objects.requireNonNull(inputStream, "输入流不能为空");
        return new InputStreamResource(inputStream);
    }

    /**
     * 根据输入流供应器创建可重复打开的资源。
     *
     * @param supplier 输入流供应器
     * @return 供应器资源
     */
    public static Resource stream(Supplier<InputStream> supplier) {
        return stream(supplier, null);
    }

    /**
     * 根据输入流供应器和文件名创建可重复打开的资源。
     *
     * @param supplier 输入流供应器
     * @param filename 文件名
     * @return 供应器资源
     */
    public static Resource stream(Supplier<InputStream> supplier, String filename) {
        Objects.requireNonNull(supplier, "输入流供应器不能为空");
        return new SupplierInputStreamResource(supplier, filename);
    }

    /**
     * 加载单个资源。
     *
     * @param location 资源定位字符串
     * @return 资源对象
     */
    public static Resource load(String location) {
        requireText(location, "资源定位字符串不能为空");
        return new DefaultResourceLoader().getResource(location);
    }

    /**
     * 加载必需存在且可读的资源。
     *
     * @param location 资源定位字符串
     * @return 资源对象
     */
    public static Resource loadRequired(String location) {
        Resource resource = load(location);
        requireReadable(resource);
        return resource;
    }

    /**
     * 根据通配符表达式加载多个资源。
     *
     * @param pattern 资源表达式
     * @return 资源列表
     */
    public static List<Resource> loadAll(String pattern) {
        requireText(pattern, "资源表达式不能为空");
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver().getResources(pattern);
            return List.of(resources);
        } catch (IOException ex) {
            throw new ResourceOperationException("加载资源失败: " + pattern, ex);
        }
    }

    /**
     * 根据通配符表达式加载第一个可读资源。
     *
     * @param pattern 资源表达式
     * @return 第一个可读资源
     */
    public static Optional<Resource> loadFirst(String pattern) {
        return loadAll(pattern).stream().filter(ResourceUtil::isReadable).findFirst();
    }

    /**
     * 从 classpath 加载资源。
     *
     * @param path classpath 路径
     * @return 资源对象
     */
    public static Resource loadClasspath(String path) {
        return classpath(path);
    }

    /**
     * 从文件系统加载资源。
     *
     * @param path 文件路径
     * @return 文件资源
     */
    public static Resource loadFile(String path) {
        return file(path);
    }

    /**
     * 从 URL 加载资源。
     *
     * @param url URL 字符串
     * @return URL 资源
     */
    public static Resource loadUrl(String url) {
        return url(url);
    }

    /**
     * 解析资源定位字符串。
     *
     * @param location 资源定位字符串
     * @return 资源对象
     */
    public static Resource resolve(String location) {
        return load(location);
    }

    /**
     * 基于已有资源解析相对资源。
     *
     * @param base         基础资源
     * @param relativePath 相对路径
     * @return 相对资源
     */
    public static Resource resolveRelative(Resource base, String relativePath) {
        Objects.requireNonNull(base, "基础资源不能为空");
        requireText(relativePath, "相对路径不能为空");
        try {
            return base.createRelative(relativePath);
        } catch (IOException ex) {
            throw new ResourceOperationException("解析相对资源失败: " + relativePath, ex);
        }
    }

    /**
     * 标准化资源定位字符串。
     *
     * @param location 资源定位字符串
     * @return 标准化后的定位字符串
     */
    public static String normalizeLocation(String location) {
        requireText(location, "资源定位字符串不能为空");
        if (location.startsWith(CLASSPATH_ALL_PREFIX)) {
            return CLASSPATH_ALL_PREFIX + cleanPath(location.substring(CLASSPATH_ALL_PREFIX.length()));
        }
        if (location.startsWith(CLASSPATH_PREFIX)) {
            return CLASSPATH_PREFIX + cleanPath(location.substring(CLASSPATH_PREFIX.length()));
        }
        if (location.startsWith(FILE_PREFIX)) {
            return FILE_PREFIX + cleanPath(location.substring(FILE_PREFIX.length()));
        }
        return cleanPath(location);
    }

    /**
     * 判断资源定位字符串是否包含通配符。
     *
     * @param location 资源定位字符串
     * @return 包含通配符返回 true
     */
    public static boolean isLocationPattern(String location) {
        if (location == null || location.isBlank()) {
            return false;
        }
        return location.indexOf('*') >= 0 || location.indexOf('?') >= 0 || location.indexOf('{') >= 0;
    }

    /**
     * 判断资源是否存在。
     *
     * @param resource 资源对象
     * @return 存在返回 true
     */
    public static boolean exists(Resource resource) {
        return resource != null && resource.exists();
    }

    /**
     * 判断资源是否不存在。
     *
     * @param resource 资源对象
     * @return 不存在返回 true
     */
    public static boolean notExists(Resource resource) {
        return !exists(resource);
    }

    /**
     * 判断资源是否可读。
     *
     * @param resource 资源对象
     * @return 可读返回 true
     */
    public static boolean isReadable(Resource resource) {
        return resource != null && resource.exists() && resource.isReadable();
    }

    /**
     * 判断资源是否可写。
     *
     * @param resource 资源对象
     * @return 可写返回 true
     */
    public static boolean isWritable(Resource resource) {
        return resource instanceof WritableResource writableResource && writableResource.isWritable();
    }

    /**
     * 判断资源是否已经打开。
     *
     * @param resource 资源对象
     * @return 已打开返回 true
     */
    public static boolean isOpen(Resource resource) {
        return resource != null && resource.isOpen();
    }

    /**
     * 判断资源是否为文件系统资源。
     *
     * @param resource 资源对象
     * @return 文件资源返回 true
     */
    public static boolean isFile(Resource resource) {
        return resource != null && resource.isFile();
    }

    /**
     * 判断资源是否为 URL 资源。
     *
     * @param resource 资源对象
     * @return URL 资源返回 true
     */
    public static boolean isUrl(Resource resource) {
        return tryGetUrl(resource).isPresent();
    }

    /**
     * 判断资源是否为 classpath 资源。
     *
     * @param resource 资源对象
     * @return classpath 资源返回 true
     */
    public static boolean isClasspath(Resource resource) {
        return resource instanceof ClassPathResource || (resource != null && resource.getDescription().contains("class path resource"));
    }

    /**
     * 判断资源是否位于 jar 文件中。
     *
     * @param resource 资源对象
     * @return jar 资源返回 true
     */
    public static boolean isJarResource(Resource resource) {
        return tryGetUrl(resource).map(URL::getProtocol).map(protocol -> "jar".equalsIgnoreCase(protocol)).orElse(false);
    }

    /**
     * 判断资源内容是否为空。
     *
     * @param resource 资源对象
     * @return 空资源返回 true
     */
    public static boolean isEmpty(Resource resource) {
        return !exists(resource) || tryContentLength(resource).orElse(0L) == 0L;
    }

    /**
     * 判断资源内容是否不为空。
     *
     * @param resource 资源对象
     * @return 非空资源返回 true
     */
    public static boolean isNotEmpty(Resource resource) {
        return !isEmpty(resource);
    }

    /**
     * 判断资源是否包含文件名。
     *
     * @param resource 资源对象
     * @return 有文件名返回 true
     */
    public static boolean hasFilename(Resource resource) {
        return resource != null && resource.getFilename() != null && !resource.getFilename().isBlank();
    }

    /**
     * 判断两个资源是否指向同一个 URI。
     *
     * @param a 第一个资源
     * @param b 第二个资源
     * @return URI 相同返回 true
     */
    public static boolean isSame(Resource a, Resource b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        Optional<URI> left = tryGetUri(a);
        Optional<URI> right = tryGetUri(b);
        return left.isPresent() && left.equals(right);
    }

    /**
     * 要求资源必须存在。
     *
     * @param resource 资源对象
     * @return 原资源对象
     */
    public static Resource requireExists(Resource resource) {
        Objects.requireNonNull(resource, "资源不能为空");
        if (!resource.exists()) {
            throw resourceNotFound(getReadableDescription(resource));
        }
        return resource;
    }

    /**
     * 要求资源必须可读。
     *
     * @param resource 资源对象
     * @return 原资源对象
     */
    public static Resource requireReadable(Resource resource) {
        requireExists(resource);
        if (!resource.isReadable()) {
            throw resourceNotReadable(resource);
        }
        return resource;
    }

    /**
     * 要求资源必须可写。
     *
     * @param resource 资源对象
     * @return 原资源对象
     */
    public static Resource requireWritable(Resource resource) {
        Objects.requireNonNull(resource, "资源不能为空");
        if (!isWritable(resource)) {
            throw new ResourceOperationException("资源不可写: " + getReadableDescription(resource));
        }
        return resource;
    }

    /**
     * 获取资源文件名。
     *
     * @param resource 资源对象
     * @return 文件名
     */
    public static String getFilename(Resource resource) {
        Objects.requireNonNull(resource, "资源不能为空");
        return resource.getFilename();
    }

    /**
     * 获取资源文件名，不存在时返回默认值。
     *
     * @param resource    资源对象
     * @param defaultName 默认文件名
     * @return 文件名
     */
    public static String getFilename(Resource resource, String defaultName) {
        String filename = resource == null ? null : resource.getFilename();
        return filename == null || filename.isBlank() ? defaultName : filename;
    }

    /**
     * 获取资源扩展名。
     *
     * @param resource 资源对象
     * @return 小写扩展名
     */
    public static String getExtension(Resource resource) {
        return getExtension(getFilename(resource, ""));
    }

    /**
     * 获取资源基础文件名。
     *
     * @param resource 资源对象
     * @return 不带扩展名的文件名
     */
    public static String getBaseName(Resource resource) {
        return removeExtension(getFilename(resource, ""));
    }

    /**
     * 获取资源描述信息。
     *
     * @param resource 资源对象
     * @return 描述信息
     */
    public static String getDescription(Resource resource) {
        Objects.requireNonNull(resource, "资源不能为空");
        return resource.getDescription();
    }

    /**
     * 获取资源大小。
     *
     * @param resource 资源对象
     * @return 字节长度
     */
    public static long getContentLength(Resource resource) {
        requireReadable(resource);
        try {
            return resource.contentLength();
        } catch (IOException ex) {
            throw new ResourceOperationException("获取资源大小失败: " + getReadableDescription(resource), ex);
        }
    }

    /**
     * 获取资源最后修改时间。
     *
     * @param resource 资源对象
     * @return 最后修改时间戳
     */
    public static long getLastModified(Resource resource) {
        requireExists(resource);
        try {
            return resource.lastModified();
        } catch (IOException ex) {
            throw new ResourceOperationException("获取资源修改时间失败: " + getReadableDescription(resource), ex);
        }
    }

    /**
     * 获取资源最后修改时间。
     *
     * @param resource 资源对象
     * @return 最后修改时间
     */
    public static Instant getLastModifiedInstant(Resource resource) {
        return Instant.ofEpochMilli(getLastModified(resource));
    }

    /**
     * 获取资源 URL。
     *
     * @param resource 资源对象
     * @return URL 对象
     */
    public static URL getUrl(Resource resource) {
        Objects.requireNonNull(resource, "资源不能为空");
        try {
            return resource.getURL();
        } catch (IOException ex) {
            throw new ResourceOperationException("获取资源 URL 失败: " + getReadableDescription(resource), ex);
        }
    }

    /**
     * 获取资源 URI。
     *
     * @param resource 资源对象
     * @return URI 对象
     */
    public static URI getUri(Resource resource) {
        Objects.requireNonNull(resource, "资源不能为空");
        try {
            return resource.getURI();
        } catch (IOException ex) {
            throw new ResourceOperationException("获取资源 URI 失败: " + getReadableDescription(resource), ex);
        }
    }

    /**
     * 获取资源路径。
     *
     * @param resource 资源对象
     * @return 资源路径
     */
    public static String getPath(Resource resource) {
        Optional<File> file = tryGetFile(resource);
        if (file.isPresent()) {
            return file.get().getPath();
        }
        return tryGetUri(resource).map(URI::toString).orElseGet(() -> getReadableDescription(resource));
    }

    /**
     * 获取资源文件对象。
     *
     * @param resource 资源对象
     * @return 文件对象
     */
    public static File getFile(Resource resource) {
        Objects.requireNonNull(resource, "资源不能为空");
        try {
            return resource.getFile();
        } catch (IOException ex) {
            throw new ResourceOperationException("资源无法转换为 File: " + getReadableDescription(resource), ex);
        }
    }

    /**
     * 获取去除扩展名后的文件名。
     *
     * @param resource 资源对象
     * @return 去除扩展名后的文件名
     */
    public static String getFileNameWithoutExtension(Resource resource) {
        return getBaseName(resource);
    }

    /**
     * 获取资源媒体类型字符串。
     *
     * @param resource 资源对象
     * @return 媒体类型
     */
    public static String getMediaType(Resource resource) {
        return getContentType(resource);
    }

    /**
     * 获取资源内容类型。
     *
     * @param resource 资源对象
     * @return 内容类型
     */
    public static String getContentType(Resource resource) {
        Objects.requireNonNull(resource, "资源不能为空");
        Optional<File> file = tryGetFile(resource);
        if (file.isPresent()) {
            try {
                String probed = Files.probeContentType(file.get().toPath());
                if (probed != null && !probed.isBlank()) {
                    return probed;
                }
            } catch (IOException ignored) {
                // 使用文件名继续推断
            }
        }
        String filename = getFilename(resource, "");
        String guessed = URLConnection.getFileNameMap().getContentTypeFor(filename);
        if (guessed != null && !guessed.isBlank()) {
            return guessed;
        }
        return manualContentType(getExtension(filename));
    }

    /**
     * 推断资源字符集。
     *
     * @param resource 资源对象
     * @return 字符集
     */
    public static Charset getCharset(Resource resource) {
        requireReadable(resource);
        try (InputStream inputStream = resource.getInputStream()) {
            byte[] head = inputStream.readNBytes(4);
            if (head.length >= 3 && (head[0] & 0xFF) == 0xEF && (head[1] & 0xFF) == 0xBB && (head[2] & 0xFF) == 0xBF) {
                return StandardCharsets.UTF_8;
            }
            if (head.length >= 2 && (head[0] & 0xFF) == 0xFE && (head[1] & 0xFF) == 0xFF) {
                return StandardCharsets.UTF_16BE;
            }
            if (head.length >= 2 && (head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xFE) {
                return StandardCharsets.UTF_16LE;
            }
            return DEFAULT_CHARSET;
        } catch (IOException ex) {
            throw new ResourceOperationException("推断资源字符集失败: " + getReadableDescription(resource), ex);
        }
    }

    /**
     * 获取适合日志输出的资源描述。
     *
     * @param resource 资源对象
     * @return 可读描述
     */
    public static String getReadableDescription(Resource resource) {
        if (resource == null) {
            return "null resource";
        }
        String filename = resource.getFilename();
        if (filename != null && !filename.isBlank()) {
            return filename + " (" + resource.getDescription() + ")";
        }
        return resource.getDescription();
    }

    /**
     * 获取资源输入流。
     *
     * @param resource 资源对象
     * @return 输入流
     */
    public static InputStream getInputStream(Resource resource) {
        requireReadable(resource);
        try {
            return resource.getInputStream();
        } catch (IOException ex) {
            throw new ResourceOperationException("打开资源输入流失败: " + getReadableDescription(resource), ex);
        }
    }

    /**
     * 读取资源为字节数组。
     *
     * @param resource 资源对象
     * @return 字节数组
     */
    public static byte[] readBytes(Resource resource) {
        requireReadable(resource);
        try (InputStream inputStream = resource.getInputStream()) {
            return inputStream.readAllBytes();
        } catch (IOException ex) {
            throw new ResourceOperationException("读取资源字节失败: " + getReadableDescription(resource), ex);
        }
    }

    /**
     * 使用 UTF-8 读取资源为字符串。
     *
     * @param resource 资源对象
     * @return 字符串内容
     */
    public static String readString(Resource resource) {
        return readString(resource, DEFAULT_CHARSET);
    }

    /**
     * 使用指定字符集读取资源为字符串。
     *
     * @param resource 资源对象
     * @param charset  字符集
     * @return 字符串内容
     */
    public static String readString(Resource resource, Charset charset) {
        Objects.requireNonNull(charset, "字符集不能为空");
        return new String(readBytes(resource), charset);
    }

    /**
     * 使用 UTF-8 读取资源为字符串。
     *
     * @param resource 资源对象
     * @return 字符串内容
     */
    public static String readUtf8String(Resource resource) {
        return readString(resource, StandardCharsets.UTF_8);
    }

    /**
     * 使用 UTF-8 按行读取资源。
     *
     * @param resource 资源对象
     * @return 行列表
     */
    public static List<String> readLines(Resource resource) {
        return readLines(resource, DEFAULT_CHARSET);
    }

    /**
     * 使用指定字符集按行读取资源。
     *
     * @param resource 资源对象
     * @param charset  字符集
     * @return 行列表
     */
    public static List<String> readLines(Resource resource, Charset charset) {
        Objects.requireNonNull(charset, "字符集不能为空");
        return readString(resource, charset).lines().toList();
    }

    /**
     * 读取资源为 Properties。
     *
     * @param resource 资源对象
     * @return Properties 对象
     */
    public static Properties readProperties(Resource resource) {
        requireReadable(resource);
        Properties properties = new Properties();
        try (InputStream inputStream = resource.getInputStream()) {
            properties.load(inputStream);
            return properties;
        } catch (IOException ex) {
            throw new ResourceOperationException("读取 Properties 失败: " + getReadableDescription(resource), ex);
        }
    }

    /**
     * 读取 JSON 文本并通过转换器转换为对象。
     *
     * @param resource  资源对象
     * @param converter JSON 文本转换器
     * @param <T>       目标类型
     * @return 转换后的对象
     */
    public static <T> T readJson(Resource resource, Function<String, T> converter) {
        return readTextObject(resource, converter);
    }

    /**
     * 读取 YAML 文本并通过转换器转换为对象。
     *
     * @param resource  资源对象
     * @param converter YAML 文本转换器
     * @param <T>       目标类型
     * @return 转换后的对象
     */
    public static <T> T readYaml(Resource resource, Function<String, T> converter) {
        return readTextObject(resource, converter);
    }

    /**
     * 读取 XML 文本并通过转换器转换为对象。
     *
     * @param resource  资源对象
     * @param converter XML 文本转换器
     * @param <T>       目标类型
     * @return 转换后的对象
     */
    public static <T> T readXml(Resource resource, Function<String, T> converter) {
        return readTextObject(resource, converter);
    }

    /**
     * 使用 JDK DOM 读取 XML 文档。
     *
     * @param resource 资源对象
     * @return XML Document
     */
    public static Document readXmlDocument(Resource resource) {
        requireReadable(resource);
        try (InputStream inputStream = resource.getInputStream()) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setExpandEntityReferences(false);
            return factory.newDocumentBuilder().parse(inputStream);
        } catch (Exception ex) {
            throw new ResourceOperationException("读取 XML 文档失败: " + getReadableDescription(resource), ex);
        }
    }

    /**
     * 通过输入流转换器读取资源对象。
     *
     * @param resource  资源对象
     * @param converter 输入流转换器
     * @param <T>       目标类型
     * @return 转换后的对象
     */
    public static <T> T readObject(Resource resource, Function<InputStream, T> converter) {
        return readWith(resource, converter);
    }

    /**
     * 使用自定义输入流读取函数读取资源。
     *
     * @param resource 资源对象
     * @param reader   输入流读取函数
     * @param <T>      返回类型
     * @return 读取结果
     */
    public static <T> T readWith(Resource resource, Function<InputStream, T> reader) {
        requireReadable(resource);
        Objects.requireNonNull(reader, "资源读取函数不能为空");
        try (InputStream inputStream = resource.getInputStream()) {
            return reader.apply(inputStream);
        } catch (IOException ex) {
            throw new ResourceOperationException("读取资源失败: " + getReadableDescription(resource), ex);
        }
    }

    /**
     * 消费资源输入流。
     *
     * @param resource 资源对象
     * @param consumer 输入流消费者
     */
    public static void consume(Resource resource, Consumer<InputStream> consumer) {
        requireReadable(resource);
        Objects.requireNonNull(consumer, "输入流消费者不能为空");
        try (InputStream inputStream = resource.getInputStream()) {
            consumer.accept(inputStream);
        } catch (IOException ex) {
            throw new ResourceOperationException("消费资源失败: " + getReadableDescription(resource), ex);
        }
    }

    /**
     * 将资源转换为字节数组。
     *
     * @param resource 资源对象
     * @return 字节数组
     */
    public static byte[] toBytes(Resource resource) {
        return readBytes(resource);
    }

    /**
     * 将资源转换为字符串。
     *
     * @param resource 资源对象
     * @param charset  字符集
     * @return 字符串内容
     */
    public static String toString(Resource resource, Charset charset) {
        return readString(resource, charset);
    }

    /**
     * 将资源转换为 File。
     *
     * @param resource 资源对象
     * @return File 对象
     */
    public static File toFile(Resource resource) {
        return getFile(resource);
    }

    /**
     * 将资源转换为 Path。
     *
     * @param resource 资源对象
     * @return Path 对象
     */
    public static Path toPath(Resource resource) {
        return toFile(resource).toPath();
    }

    /**
     * 将资源转换为 URL。
     *
     * @param resource 资源对象
     * @return URL 对象
     */
    public static URL toUrl(Resource resource) {
        return getUrl(resource);
    }

    /**
     * 将资源转换为 URI。
     *
     * @param resource 资源对象
     * @return URI 对象
     */
    public static URI toUri(Resource resource) {
        return getUri(resource);
    }

    /**
     * 将资源转换为输入流。
     *
     * @param resource 资源对象
     * @return 输入流
     */
    public static InputStream toInputStream(Resource resource) {
        return getInputStream(resource);
    }

    /**
     * 将资源转换为字节数组资源。
     *
     * @param resource 资源对象
     * @return 字节数组资源
     */
    public static ByteArrayResource toByteArrayResource(Resource resource) {
        requireReadable(resource);
        return new NamedByteArrayResource(readBytes(resource), getFilename(resource, null), getReadableDescription(resource));
    }

    /**
     * 将资源转换为输入流资源。
     *
     * @param resource 资源对象
     * @return 输入流资源
     */
    public static InputStreamResource toInputStreamResource(Resource resource) {
        return new InputStreamResource(getInputStream(resource), getReadableDescription(resource));
    }

    /**
     * 将资源转换为文件系统资源。
     *
     * @param resource 资源对象
     * @return 文件系统资源
     */
    public static FileSystemResource toFileSystemResource(Resource resource) {
        return new FileSystemResource(toFile(resource));
    }

    /**
     * 创建资源表单文件包装对象。
     *
     * @param resource 资源对象
     * @return 表单文件包装对象
     */
    public static ResourceMultipartFile toMultipartFile(Resource resource) {
        requireReadable(resource);
        return new ResourceMultipartFile(resource);
    }

    /**
     * 创建资源响应描述对象。
     *
     * @param resource 资源对象
     * @return 资源响应描述对象
     */
    public static ResourceHttpResponse toHttpEntity(Resource resource) {
        return asResponseEntity(resource);
    }

    /**
     * 创建资源分片描述对象。
     *
     * @param resource 资源对象
     * @param position 起始位置
     * @param count    读取长度
     * @return 资源分片描述对象
     */
    public static ResourceRegion toResourceRegion(Resource resource, long position, long count) {
        return getRangeRegion(resource, position, count);
    }

    /**
     * 安全转换资源为 File。
     *
     * @param resource 资源对象
     * @return File 可选值
     */
    public static Optional<File> tryToFile(Resource resource) {
        return tryGetFile(resource);
    }

    /**
     * 将资源复制到输出流。
     *
     * @param source 源资源
     * @param target 目标输出流
     * @return 复制字节数
     */
    public static long copy(Resource source, OutputStream target) {
        requireReadable(source);
        Objects.requireNonNull(target, "目标输出流不能为空");
        try (InputStream inputStream = source.getInputStream()) {
            return inputStream.transferTo(target);
        } catch (IOException ex) {
            throw new ResourceOperationException("复制资源失败: " + getReadableDescription(source), ex);
        }
    }

    /**
     * 将资源复制到目标文件。
     *
     * @param source 源资源
     * @param target 目标文件
     * @return 目标文件
     */
    public static File copy(Resource source, File target) {
        Objects.requireNonNull(target, "目标文件不能为空");
        copy(source, target.toPath());
        return target;
    }

    /**
     * 将资源复制到目标路径。
     *
     * @param source 源资源
     * @param target 目标路径
     * @return 目标路径
     */
    public static Path copy(Resource source, Path target) {
        requireReadable(source);
        Objects.requireNonNull(target, "目标路径不能为空");
        createParentDirectories(target);
        try (InputStream inputStream = source.getInputStream()) {
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            return target;
        } catch (IOException ex) {
            throw new ResourceOperationException("复制资源到文件失败: " + target, ex);
        }
    }

    /**
     * 将资源复制到目录，使用资源原文件名。
     *
     * @param source    源资源
     * @param directory 目标目录
     * @return 目标文件路径
     */
    public static Path copyToDirectory(Resource source, Path directory) {
        requireReadable(source);
        Objects.requireNonNull(directory, "目标目录不能为空");
        String filename = getFilename(source, null);
        requireText(filename, "资源文件名不能为空");
        return saveAs(source, directory, filename);
    }

    /**
     * 批量复制资源到目录。
     *
     * @param sources   源资源集合
     * @param directory 目标目录
     * @return 目标文件路径列表
     */
    public static List<Path> copyAll(Collection<Resource> sources, Path directory) {
        Objects.requireNonNull(sources, "资源集合不能为空");
        Objects.requireNonNull(directory, "目标目录不能为空");
        List<Path> paths = new ArrayList<>();
        for (Resource source : sources) {
            paths.add(copyToDirectory(source, directory));
        }
        return paths;
    }

    /**
     * 将资源内容写入目标文件。
     *
     * @param resource 源资源
     * @param target   目标路径
     * @return 目标路径
     */
    public static Path writeBytes(Resource resource, Path target) {
        return copy(resource, target);
    }

    /**
     * 读取资源文本并写入目标文件。
     *
     * @param resource 源资源
     * @param target   目标路径
     * @param charset  字符集
     * @return 目标路径
     */
    public static Path writeString(Resource resource, Path target, Charset charset) {
        Objects.requireNonNull(target, "目标路径不能为空");
        Objects.requireNonNull(charset, "字符集不能为空");
        createParentDirectories(target);
        try {
            Files.writeString(target, readString(resource, charset), charset);
            return target;
        } catch (IOException ex) {
            throw new ResourceOperationException("写入字符串资源失败: " + target, ex);
        }
    }

    /**
     * 保存资源到指定路径。
     *
     * @param resource 源资源
     * @param target   目标路径
     * @return 目标路径
     */
    public static Path save(Resource resource, Path target) {
        return copy(resource, target);
    }

    /**
     * 使用指定文件名保存资源到目录。
     *
     * @param resource  源资源
     * @param directory 目标目录
     * @param filename  目标文件名
     * @return 目标路径
     */
    public static Path saveAs(Resource resource, Path directory, String filename) {
        requireReadable(resource);
        Objects.requireNonNull(directory, "目标目录不能为空");
        requireText(filename, "目标文件名不能为空");
        checkFilenameSafe(filename);
        try {
            Files.createDirectories(directory);
        } catch (IOException ex) {
            throw new ResourceOperationException("创建目录失败: " + directory, ex);
        }
        return copy(resource, directory.resolve(filename));
    }

    /**
     * 将资源传输到输出流。
     *
     * @param resource     源资源
     * @param outputStream 输出流
     * @return 传输字节数
     */
    public static long transferTo(Resource resource, OutputStream outputStream) {
        return copy(resource, outputStream);
    }

    /**
     * 将资源释放到本地文件。
     *
     * @param resource 源资源
     * @param target   目标路径
     * @return 目标路径
     */
    public static Path extract(Resource resource, Path target) {
        return copy(resource, target);
    }

    /**
     * 将 classpath 资源释放到本地文件。
     *
     * @param path   classpath 路径
     * @param target 目标路径
     * @return 目标路径
     */
    public static Path extractClasspath(String path, Path target) {
        return extract(loadClasspath(path), target);
    }

    /**
     * 根据表达式扫描资源。
     *
     * @param pattern 资源表达式
     * @return 资源列表
     */
    public static List<Resource> scan(String pattern) {
        return loadAll(pattern);
    }

    /**
     * 扫描 classpath 资源。
     *
     * @param pattern classpath 相对表达式
     * @return 资源列表
     */
    public static List<Resource> scanClasspath(String pattern) {
        requireText(pattern, "classpath 表达式不能为空");
        String normalized = pattern.startsWith(CLASSPATH_ALL_PREFIX) || pattern.startsWith(CLASSPATH_PREFIX)
                ? pattern
                : CLASSPATH_ALL_PREFIX + pattern;
        return loadAll(normalized);
    }

    /**
     * 扫描文件系统资源。
     *
     * @param pattern 文件资源表达式
     * @return 资源列表
     */
    public static List<Resource> scanFiles(String pattern) {
        requireText(pattern, "文件资源表达式不能为空");
        return loadAll(pattern.startsWith(FILE_PREFIX) ? pattern : FILE_PREFIX + pattern);
    }

    /**
     * 查找匹配表达式的资源。
     *
     * @param pattern 资源表达式
     * @return 资源列表
     */
    public static List<Resource> find(String pattern) {
        return scan(pattern);
    }

    /**
     * 查找第一个匹配资源。
     *
     * @param pattern 资源表达式
     * @return 第一个匹配资源
     */
    public static Optional<Resource> findFirst(String pattern) {
        return loadFirst(pattern);
    }

    /**
     * 按扩展名查找资源。
     *
     * @param pattern   资源表达式
     * @param extension 扩展名
     * @return 资源列表
     */
    public static List<Resource> findByExtension(String pattern, String extension) {
        requireText(extension, "扩展名不能为空");
        String expected = normalizeExtension(extension);
        return filter(scan(pattern), resource -> expected.equals(getExtension(resource)));
    }

    /**
     * 按文件名查找资源。
     *
     * @param pattern  资源表达式
     * @param filename 文件名
     * @return 资源列表
     */
    public static List<Resource> findByFilename(String pattern, String filename) {
        requireText(filename, "文件名不能为空");
        return filter(scan(pattern), resource -> filename.equals(resource.getFilename()));
    }

    /**
     * 使用自定义断言过滤资源。
     *
     * @param resources 资源集合
     * @param predicate 过滤条件
     * @return 过滤结果
     */
    public static List<Resource> filter(Collection<Resource> resources, Predicate<Resource> predicate) {
        Objects.requireNonNull(resources, "资源集合不能为空");
        Objects.requireNonNull(predicate, "过滤条件不能为空");
        return resources.stream().filter(Objects::nonNull).filter(predicate).toList();
    }

    /**
     * 过滤可读资源。
     *
     * @param resources 资源集合
     * @return 可读资源列表
     */
    public static List<Resource> filterReadable(Collection<Resource> resources) {
        return filter(resources, ResourceUtil::isReadable);
    }

    /**
     * 过滤存在的资源。
     *
     * @param resources 资源集合
     * @return 存在的资源列表
     */
    public static List<Resource> filterExists(Collection<Resource> resources) {
        return filter(resources, ResourceUtil::exists);
    }

    /**
     * 按文件名排序资源。
     *
     * @param resources 资源集合
     * @return 排序后的资源列表
     */
    public static List<Resource> sortByFilename(Collection<Resource> resources) {
        Objects.requireNonNull(resources, "资源集合不能为空");
        return resources.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(resource -> getFilename(resource, "")))
                .toList();
    }

    /**
     * 按最后修改时间排序资源。
     *
     * @param resources 资源集合
     * @return 排序后的资源列表
     */
    public static List<Resource> sortByLastModified(Collection<Resource> resources) {
        Objects.requireNonNull(resources, "资源集合不能为空");
        return resources.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingLong(resource -> tryLastModified(resource).orElse(0L)))
                .toList();
    }

    /**
     * 按扩展名分组资源。
     *
     * @param resources 资源集合
     * @return 扩展名分组结果
     */
    public static Map<String, List<Resource>> groupByExtension(Collection<Resource> resources) {
        Objects.requireNonNull(resources, "资源集合不能为空");
        Map<String, List<Resource>> result = new LinkedHashMap<>();
        for (Resource resource : resources) {
            if (resource == null) {
                continue;
            }
            result.computeIfAbsent(getExtension(resource), key -> new ArrayList<>()).add(resource);
        }
        return result;
    }

    /**
     * 将资源按文件名转换为 Map。
     *
     * @param resources 资源集合
     * @return 文件名到资源的映射
     */
    public static Map<String, Resource> mapByFilename(Collection<Resource> resources) {
        Objects.requireNonNull(resources, "资源集合不能为空");
        Map<String, Resource> result = new LinkedHashMap<>();
        for (Resource resource : resources) {
            if (resource == null || resource.getFilename() == null) {
                continue;
            }
            result.putIfAbsent(resource.getFilename(), resource);
        }
        return result;
    }

    /**
     * 清理路径中的冗余分隔符和相对段。
     *
     * @param path 路径
     * @return 清理后的路径
     */
    public static String cleanPath(String path) {
        requireText(path, "路径不能为空");
        String normalized = path.replace('\\', '/');
        String prefix = "";
        Matcher matcher = PROTOCOL_PATTERN.matcher(normalized);
        if (matcher.matches()) {
            prefix = matcher.group(1) + ":";
            normalized = normalized.substring(prefix.length());
        }
        boolean absolute = normalized.startsWith("/");
        String[] parts = normalized.split("/+");
        ArrayDeque<String> stack = new ArrayDeque<>();
        for (String part : parts) {
            if (part.isEmpty() || ".".equals(part)) {
                continue;
            }
            if ("..".equals(part)) {
                if (!stack.isEmpty() && !"..".equals(stack.peekLast())) {
                    stack.removeLast();
                } else if (!absolute) {
                    stack.addLast(part);
                }
            } else {
                stack.addLast(part);
            }
        }
        String cleaned = String.join("/", stack);
        if (absolute) {
            cleaned = "/" + cleaned;
        }
        return prefix + cleaned;
    }

    /**
     * 标准化路径。
     *
     * @param path 路径
     * @return 标准化后的路径
     */
    public static String normalizePath(String path) {
        return cleanPath(path);
    }

    /**
     * 获取父路径。
     *
     * @param path 路径
     * @return 父路径
     */
    public static String getParentPath(String path) {
        requireText(path, "路径不能为空");
        String normalized = cleanPath(path);
        int index = normalized.lastIndexOf('/');
        if (index < 0) {
            return "";
        }
        if (index == 0) {
            return "/";
        }
        return normalized.substring(0, index);
    }

    /**
     * 从路径中获取文件名。
     *
     * @param path 路径
     * @return 文件名
     */
    public static String getFilename(String path) {
        requireText(path, "路径不能为空");
        String normalized = path.replace('\\', '/');
        int index = normalized.lastIndexOf('/');
        return index >= 0 ? normalized.substring(index + 1) : normalized;
    }

    /**
     * 从路径或文件名中获取扩展名。
     *
     * @param path 路径或文件名
     * @return 小写扩展名
     */
    public static String getExtension(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        String filename = getFilename(path);
        int index = filename.lastIndexOf('.');
        if (index < 0 || index == filename.length() - 1) {
            return "";
        }
        return filename.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    /**
     * 移除文件扩展名。
     *
     * @param filename 文件名
     * @return 移除扩展名后的文件名
     */
    public static String removeExtension(String filename) {
        if (filename == null || filename.isBlank()) {
            return filename;
        }
        int index = filename.lastIndexOf('.');
        return index > 0 ? filename.substring(0, index) : filename;
    }

    /**
     * 修改文件扩展名。
     *
     * @param filename  文件名
     * @param extension 新扩展名
     * @return 修改后的文件名
     */
    public static String changeExtension(String filename, String extension) {
        requireText(filename, "文件名不能为空");
        requireText(extension, "扩展名不能为空");
        return removeExtension(filename) + "." + normalizeExtension(extension);
    }

    /**
     * 添加 classpath 前缀。
     *
     * @param path classpath 路径
     * @return 带 classpath 前缀的路径
     */
    public static String appendClasspathPrefix(String path) {
        requireText(path, "路径不能为空");
        return path.startsWith(CLASSPATH_PREFIX) || path.startsWith(CLASSPATH_ALL_PREFIX) ? path : CLASSPATH_PREFIX + path;
    }

    /**
     * 移除 classpath 前缀。
     *
     * @param path 路径
     * @return 移除前缀后的路径
     */
    public static String removeClasspathPrefix(String path) {
        requireText(path, "路径不能为空");
        if (path.startsWith(CLASSPATH_ALL_PREFIX)) {
            return path.substring(CLASSPATH_ALL_PREFIX.length());
        }
        if (path.startsWith(CLASSPATH_PREFIX)) {
            return path.substring(CLASSPATH_PREFIX.length());
        }
        return path;
    }

    /**
     * 判断资源定位字符串是否包含协议。
     *
     * @param location 资源定位字符串
     * @return 包含协议返回 true
     */
    public static boolean hasProtocol(String location) {
        return location != null && PROTOCOL_PATTERN.matcher(location).matches();
    }

    /**
     * 获取资源定位字符串协议。
     *
     * @param location 资源定位字符串
     * @return 协议可选值
     */
    public static Optional<String> getProtocol(String location) {
        if (location == null || location.isBlank()) {
            return Optional.empty();
        }
        Matcher matcher = PROTOCOL_PATTERN.matcher(location);
        return matcher.matches() ? Optional.of(matcher.group(1).toLowerCase(Locale.ROOT)) : Optional.empty();
    }

    /**
     * 拼接路径片段。
     *
     * @param paths 路径片段
     * @return 拼接后的路径
     */
    public static String joinPath(String... paths) {
        Objects.requireNonNull(paths, "路径片段不能为空");
        List<String> parts = new ArrayList<>();
        for (String path : paths) {
            if (path == null || path.isBlank()) {
                continue;
            }
            String item = path.replace('\\', '/');
            item = trimSlashes(item);
            if (!item.isBlank()) {
                parts.add(item);
            }
        }
        return String.join("/", parts);
    }

    /**
     * 获取目标路径相对于基础路径的相对路径。
     *
     * @param basePath   基础路径
     * @param targetPath 目标路径
     * @return 相对路径
     */
    public static String relativePath(String basePath, String targetPath) {
        requireText(basePath, "基础路径不能为空");
        requireText(targetPath, "目标路径不能为空");
        try {
            Path base = Paths.get(basePath).normalize();
            Path target = Paths.get(targetPath).normalize();
            return base.relativize(target).toString().replace('\\', '/');
        } catch (IllegalArgumentException ex) {
            throw new ResourceOperationException("计算相对路径失败", ex);
        }
    }

    /**
     * 校验资源必须存在。
     *
     * @param resource 资源对象
     */
    public static void checkExists(Resource resource) {
        requireExists(resource);
    }

    /**
     * 校验资源必须可读。
     *
     * @param resource 资源对象
     */
    public static void checkReadable(Resource resource) {
        requireReadable(resource);
    }

    /**
     * 校验资源必须可写。
     *
     * @param resource 资源对象
     */
    public static void checkWritable(Resource resource) {
        requireWritable(resource);
    }

    /**
     * 校验资源内容不能为空。
     *
     * @param resource 资源对象
     */
    public static void checkNotEmpty(Resource resource) {
        if (isEmpty(resource)) {
            throw new ResourceOperationException("资源内容不能为空: " + getReadableDescription(resource));
        }
    }

    /**
     * 校验资源最大大小。
     *
     * @param resource 资源对象
     * @param maxSize  最大字节数
     */
    public static void checkMaxSize(Resource resource, long maxSize) {
        if (maxSize < 0) {
            throw new IllegalArgumentException("最大字节数不能小于 0");
        }
        long length = getContentLength(resource);
        if (length > maxSize) {
            throw new ResourceOperationException("资源大小超过限制: " + length + " > " + maxSize);
        }
    }

    /**
     * 校验资源扩展名。
     *
     * @param resource   资源对象
     * @param extensions 允许的扩展名
     */
    public static void checkExtension(Resource resource, String... extensions) {
        if (!isAllowedExtension(resource, extensions)) {
            throw new ResourceOperationException("资源扩展名不允许: " + getExtension(resource));
        }
    }

    /**
     * 校验资源内容类型。
     *
     * @param resource     资源对象
     * @param contentTypes 允许的内容类型
     */
    public static void checkContentType(Resource resource, String... contentTypes) {
        Objects.requireNonNull(contentTypes, "内容类型不能为空");
        String actual = getContentType(resource);
        for (String contentType : contentTypes) {
            if (contentType != null && contentType.equalsIgnoreCase(actual)) {
                return;
            }
        }
        throw new ResourceOperationException("资源内容类型不允许: " + actual);
    }

    /**
     * 校验资源文件名合法。
     *
     * @param resource 资源对象
     */
    public static void checkFilename(Resource resource) {
        checkFilenameSafe(getFilename(resource));
    }

    /**
     * 使用自定义校验器校验资源。
     *
     * @param resource  资源对象
     * @param validator 校验器
     */
    public static void validate(Resource resource, ResourceValidator validator) {
        Objects.requireNonNull(resource, "资源不能为空");
        Objects.requireNonNull(validator, "资源校验器不能为空");
        validator.validate(resource);
    }

    /**
     * 要求资源满足指定条件。
     *
     * @param resource  资源对象
     * @param predicate 条件
     * @param message   异常消息
     * @return 原资源对象
     */
    public static Resource require(Resource resource, Predicate<Resource> predicate, String message) {
        Objects.requireNonNull(resource, "资源不能为空");
        Objects.requireNonNull(predicate, "资源条件不能为空");
        if (!predicate.test(resource)) {
            throw new ResourceOperationException(message == null || message.isBlank() ? "资源条件不满足" : message);
        }
        return resource;
    }

    /**
     * 判断路径是否安全。
     *
     * @param path 路径
     * @return 安全返回 true
     */
    public static boolean isSafePath(String path) {
        return path != null && !path.isBlank() && !isPathTraversal(path) && path.indexOf('\0') < 0;
    }

    /**
     * 校验路径安全。
     *
     * @param path 路径
     */
    public static void checkSafePath(String path) {
        if (!isSafePath(path)) {
            throw new ResourceOperationException("路径不安全: " + path);
        }
    }

    /**
     * 判断路径是否包含路径穿越风险。
     *
     * @param path 路径
     * @return 包含路径穿越返回 true
     */
    public static boolean isPathTraversal(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }
        String normalized = path.replace('\\', '/');
        return normalized.equals("..") || normalized.startsWith("../") || normalized.contains("/../") || normalized.endsWith("/..");
    }

    /**
     * 清理文件名中的不安全字符。
     *
     * @param filename 文件名
     * @return 清理后的文件名
     */
    public static String cleanFilename(String filename) {
        requireText(filename, "文件名不能为空");
        String cleaned = filename.replace('\\', '_').replace('/', '_').replace('\0', '_');
        cleaned = cleaned.replaceAll("[\\r\\n\\t]", "_");
        cleaned = cleaned.replaceAll("[:*?\"<>|]", "_");
        while (cleaned.contains("..")) {
            cleaned = cleaned.replace("..", ".");
        }
        return cleaned;
    }

    /**
     * 校验文件名安全。
     *
     * @param filename 文件名
     */
    public static void checkFilenameSafe(String filename) {
        requireText(filename, "文件名不能为空");
        if (!filename.equals(cleanFilename(filename))) {
            throw new ResourceOperationException("文件名不安全: " + filename);
        }
    }

    /**
     * 判断资源扩展名是否在白名单中。
     *
     * @param resource   资源对象
     * @param extensions 允许的扩展名
     * @return 允许返回 true
     */
    public static boolean isAllowedExtension(Resource resource, String... extensions) {
        Objects.requireNonNull(extensions, "扩展名不能为空");
        Set<String> allowed = normalizeExtensions(extensions);
        return allowed.contains(getExtension(resource));
    }

    /**
     * 判断资源扩展名是否在黑名单中。
     *
     * @param resource   资源对象
     * @param extensions 禁止的扩展名
     * @return 禁止返回 true
     */
    public static boolean isDeniedExtension(Resource resource, String... extensions) {
        Objects.requireNonNull(extensions, "扩展名不能为空");
        Set<String> denied = normalizeExtensions(extensions);
        return denied.contains(getExtension(resource));
    }

    /**
     * 校验资源扩展名在白名单中。
     *
     * @param resource   资源对象
     * @param extensions 允许的扩展名
     */
    public static void checkAllowedExtension(Resource resource, String... extensions) {
        checkExtension(resource, extensions);
    }

    /**
     * 校验资源内容长度。
     *
     * @param resource 资源对象
     * @param maxSize  最大字节数
     */
    public static void checkContentLength(Resource resource, long maxSize) {
        checkMaxSize(resource, maxSize);
    }

    /**
     * 校验 URL 资源主机在白名单中。
     *
     * @param resource     资源对象
     * @param allowedHosts 允许的主机名集合
     */
    public static void checkTrustedUrl(Resource resource, Collection<String> allowedHosts) {
        Objects.requireNonNull(allowedHosts, "允许的主机名集合不能为空");
        URL url = getUrl(resource);
        String host = url.getHost();
        boolean trusted = allowedHosts.stream().filter(Objects::nonNull).anyMatch(item -> item.equalsIgnoreCase(host));
        if (!trusted) {
            throw new ResourceOperationException("URL 主机不可信: " + host);
        }
    }

    /**
     * 清理资源定位字符串。
     *
     * @param location 资源定位字符串
     * @return 清理后的定位字符串
     */
    public static String sanitizeLocation(String location) {
        requireText(location, "资源定位字符串不能为空");
        String sanitized = location.trim().replace("\0", "");
        if (sanitized.startsWith(CLASSPATH_ALL_PREFIX)) {
            return CLASSPATH_ALL_PREFIX + cleanPath(sanitized.substring(CLASSPATH_ALL_PREFIX.length()));
        }
        if (sanitized.startsWith(CLASSPATH_PREFIX)) {
            return CLASSPATH_PREFIX + cleanPath(sanitized.substring(CLASSPATH_PREFIX.length()));
        }
        return cleanPath(sanitized);
    }

    /**
     * 构造下载响应描述。
     *
     * @param resource 资源对象
     * @return 响应描述
     */
    public static ResourceHttpResponse asDownload(Resource resource) {
        return asDownload(resource, getFilename(resource, "download.bin"));
    }

    /**
     * 构造指定文件名的下载响应描述。
     *
     * @param resource 资源对象
     * @param filename 下载文件名
     * @return 响应描述
     */
    public static ResourceHttpResponse asDownload(Resource resource, String filename) {
        requireReadable(resource);
        requireText(filename, "下载文件名不能为空");
        Map<String, String> headers = asAttachmentHeaders(filename);
        headers.put("Content-Type", getContentType(resource));
        headers.put("Content-Length", String.valueOf(getContentLength(resource)));
        return new ResourceHttpResponse(resource, 200, headers, getContentType(resource));
    }

    /**
     * 构造内联预览响应描述。
     *
     * @param resource 资源对象
     * @return 响应描述
     */
    public static ResourceHttpResponse asInline(Resource resource) {
        return asInline(resource, getFilename(resource, "resource"));
    }

    /**
     * 构造指定文件名的内联预览响应描述。
     *
     * @param resource 资源对象
     * @param filename 文件名
     * @return 响应描述
     */
    public static ResourceHttpResponse asInline(Resource resource, String filename) {
        requireReadable(resource);
        requireText(filename, "文件名不能为空");
        Map<String, String> headers = asInlineHeaders(filename);
        headers.put("Content-Type", getContentType(resource));
        headers.put("Content-Length", String.valueOf(getContentLength(resource)));
        return new ResourceHttpResponse(resource, 200, headers, getContentType(resource));
    }

    /**
     * 构造通用资源响应描述。
     *
     * @param resource 资源对象
     * @return 响应描述
     */
    public static ResourceHttpResponse asResponseEntity(Resource resource) {
        requireReadable(resource);
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Content-Type", getContentType(resource));
        headers.put("Content-Length", String.valueOf(getContentLength(resource)));
        return new ResourceHttpResponse(resource, 200, headers, getContentType(resource));
    }

    /**
     * 构造通用资源响应描述。
     *
     * @param resource    资源对象
     * @param contentType 内容类型
     * @return 响应描述
     */
    public static ResourceHttpResponse asResponseEntity(Resource resource, String contentType) {
        requireReadable(resource);
        requireText(contentType, "内容类型不能为空");
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Content-Type", contentType);
        headers.put("Content-Length", String.valueOf(getContentLength(resource)));
        return new ResourceHttpResponse(resource, 200, headers, contentType);
    }

    /**
     * 构造附件下载响应头。
     *
     * @param resource 资源对象
     * @return 响应头
     */
    public static Map<String, String> asAttachmentHeaders(Resource resource) {
        return asAttachmentHeaders(getFilename(resource, "download.bin"));
    }

    /**
     * 构造附件下载响应头。
     *
     * @param filename 文件名
     * @return 响应头
     */
    public static Map<String, String> asAttachmentHeaders(String filename) {
        requireText(filename, "文件名不能为空");
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Content-Disposition", getContentDisposition("attachment", filename));
        return headers;
    }

    /**
     * 构造内联预览响应头。
     *
     * @param resource 资源对象
     * @return 响应头
     */
    public static Map<String, String> asInlineHeaders(Resource resource) {
        return asInlineHeaders(getFilename(resource, "resource"));
    }

    /**
     * 构造内联预览响应头。
     *
     * @param filename 文件名
     * @return 响应头
     */
    public static Map<String, String> asInlineHeaders(String filename) {
        requireText(filename, "文件名不能为空");
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Content-Disposition", getContentDisposition("inline", filename));
        return headers;
    }

    /**
     * 构造附件下载 Content-Disposition。
     *
     * @param filename 文件名
     * @return Content-Disposition 值
     */
    public static String getContentDisposition(String filename) {
        return getContentDisposition("attachment", filename);
    }

    /**
     * 构造 Content-Disposition。
     *
     * @param disposition 展示方式
     * @param filename    文件名
     * @return Content-Disposition 值
     */
    public static String getContentDisposition(String disposition, String filename) {
        requireText(disposition, "展示方式不能为空");
        requireText(filename, "文件名不能为空");
        String safeFilename = cleanFilename(filename);
        String encoded = java.net.URLEncoder.encode(safeFilename, StandardCharsets.UTF_8).replace("+", "%20");
        return disposition + "; filename=\"" + safeFilename.replace("\"", "") + "\"; filename*=UTF-8''" + encoded;
    }

    /**
     * 获取资源分片描述。
     *
     * @param resource 资源对象
     * @param position 起始位置
     * @param count    分片长度
     * @return 资源分片
     */
    public static ResourceRegion getRangeRegion(Resource resource, long position, long count) {
        requireReadable(resource);
        if (position < 0) {
            throw new IllegalArgumentException("起始位置不能小于 0");
        }
        if (count < 0) {
            throw new IllegalArgumentException("分片长度不能小于 0");
        }
        long length = getContentLength(resource);
        if (position > length) {
            throw new IllegalArgumentException("起始位置不能大于资源长度");
        }
        long actualCount = Math.min(count, length - position);
        return new ResourceRegion(resource, position, actualCount);
    }

    /**
     * 判断资源是否支持分片读取。
     *
     * @param resource 资源对象
     * @return 支持返回 true
     */
    public static boolean supportsRange(Resource resource) {
        return isReadable(resource) && tryContentLength(resource).orElse(-1L) >= 0;
    }

    /**
     * 读取 UTF-8 模板文本。
     *
     * @param location 资源定位字符串
     * @return 模板文本
     */
    public static String readTemplate(String location) {
        return readTemplate(location, DEFAULT_CHARSET);
    }

    /**
     * 读取指定字符集的模板文本。
     *
     * @param location 资源定位字符串
     * @param charset  字符集
     * @return 模板文本
     */
    public static String readTemplate(String location, Charset charset) {
        return readString(loadRequired(location), charset);
    }

    /**
     * 读取 JSON 配置文本并转换为对象。
     *
     * @param location  资源定位字符串
     * @param converter JSON 文本转换器
     * @param <T>       目标类型
     * @return 转换后的对象
     */
    public static <T> T readJsonConfig(String location, Function<String, T> converter) {
        return readJson(loadRequired(location), converter);
    }

    /**
     * 读取 YAML 配置文本并转换为对象。
     *
     * @param location  资源定位字符串
     * @param converter YAML 文本转换器
     * @param <T>       目标类型
     * @return 转换后的对象
     */
    public static <T> T readYamlConfig(String location, Function<String, T> converter) {
        return readYaml(loadRequired(location), converter);
    }

    /**
     * 读取 Properties 配置。
     *
     * @param location 资源定位字符串
     * @return Properties 对象
     */
    public static Properties readPropertiesConfig(String location) {
        return readProperties(loadRequired(location));
    }

    /**
     * 读取 SQL 文件文本。
     *
     * @param location 资源定位字符串
     * @return SQL 文本
     */
    public static String readSql(String location) {
        return readString(loadRequired(location), DEFAULT_CHARSET);
    }

    /**
     * 读取 XML 文件文本。
     *
     * @param location 资源定位字符串
     * @return XML 文本
     */
    public static String readXml(String location) {
        return readString(loadRequired(location), DEFAULT_CHARSET);
    }

    /**
     * 批量加载模板资源。
     *
     * @param pattern 模板表达式
     * @return 模板资源列表
     */
    public static List<Resource> loadTemplates(String pattern) {
        return loadAll(pattern);
    }

    /**
     * 批量加载配置资源。
     *
     * @param pattern 配置表达式
     * @return 配置资源列表
     */
    public static List<Resource> loadConfigs(String pattern) {
        return loadAll(pattern);
    }

    /**
     * 使用简单变量替换渲染模板。
     *
     * @param resource  模板资源
     * @param variables 变量映射
     * @return 渲染后的文本
     */
    public static String renderTemplate(Resource resource, Map<String, ?> variables) {
        Objects.requireNonNull(variables, "模板变量不能为空");
        String template = readString(resource, DEFAULT_CHARSET);
        Matcher matcher = TEMPLATE_VARIABLE_PATTERN.matcher(template);
        StringBuilder builder = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1).trim();
            Object value = variables.get(key);
            matcher.appendReplacement(builder, Matcher.quoteReplacement(value == null ? "" : String.valueOf(value)));
        }
        matcher.appendTail(builder);
        return builder.toString();
    }

    /**
     * 缓存资源内容并返回可重复读取资源。
     *
     * @param resource 资源对象
     * @return 可重复读取资源
     */
    public static Resource cache(Resource resource) {
        requireReadable(resource);
        byte[] bytes = readBytes(resource);
        RESOURCE_CACHE.put(cacheKey(resource), Arrays.copyOf(bytes, bytes.length));
        return new NamedByteArrayResource(bytes, getFilename(resource, null), getReadableDescription(resource));
    }

    /**
     * 必要时缓存资源内容。
     *
     * @param resource 资源对象
     * @return 可重复读取资源
     */
    public static Resource cacheIfNecessary(Resource resource) {
        return isRepeatable(resource) ? resource : cache(resource);
    }

    /**
     * 判断资源是否可重复读取。
     *
     * @param resource 资源对象
     * @return 可重复读取返回 true
     */
    public static boolean isRepeatable(Resource resource) {
        return resource != null && !resource.isOpen();
    }

    /**
     * 将资源转换为可重复读取资源。
     *
     * @param resource 资源对象
     * @return 可重复读取资源
     */
    public static Resource toRepeatable(Resource resource) {
        return cacheIfNecessary(resource);
    }

    /**
     * 将资源缓存为字节数组。
     *
     * @param resource 资源对象
     * @return 缓存字节数组
     */
    public static byte[] toCachedBytes(Resource resource) {
        requireReadable(resource);
        String key = cacheKey(resource);
        return Arrays.copyOf(RESOURCE_CACHE.computeIfAbsent(key, ignored -> readBytes(resource)), RESOURCE_CACHE.get(key).length);
    }

    /**
     * 包装为可重复读取资源。
     *
     * @param resource 资源对象
     * @return 可重复读取资源
     */
    public static Resource wrapRepeatable(Resource resource) {
        return toRepeatable(resource);
    }

    /**
     * 清除指定资源缓存。
     *
     * @param key 缓存键
     */
    public static void clearCache(String key) {
        requireText(key, "缓存键不能为空");
        RESOURCE_CACHE.remove(key);
    }

    /**
     * 清除全部资源缓存。
     */
    public static void clearAllCache() {
        RESOURCE_CACHE.clear();
    }

    /**
     * 安全读取字节数组。
     *
     * @param resource 资源对象
     * @return 字节数组可选值
     */
    public static Optional<byte[]> tryReadBytes(Resource resource) {
        try {
            return Optional.of(readBytes(resource));
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    /**
     * 安全读取字符串。
     *
     * @param resource 资源对象
     * @param charset  字符集
     * @return 字符串可选值
     */
    public static Optional<String> tryReadString(Resource resource, Charset charset) {
        try {
            return Optional.of(readString(resource, charset));
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    /**
     * 安全获取资源文件。
     *
     * @param resource 资源对象
     * @return 文件可选值
     */
    public static Optional<File> tryGetFile(Resource resource) {
        if (resource == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(resource.getFile());
        } catch (IOException | RuntimeException ex) {
            return Optional.empty();
        }
    }

    /**
     * 安全获取资源 URL。
     *
     * @param resource 资源对象
     * @return URL 可选值
     */
    public static Optional<URL> tryGetUrl(Resource resource) {
        if (resource == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(resource.getURL());
        } catch (IOException | RuntimeException ex) {
            return Optional.empty();
        }
    }

    /**
     * 安全获取资源 URI。
     *
     * @param resource 资源对象
     * @return URI 可选值
     */
    public static Optional<URI> tryGetUri(Resource resource) {
        if (resource == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(resource.getURI());
        } catch (IOException | RuntimeException ex) {
            return Optional.empty();
        }
    }

    /**
     * 安全获取资源大小。
     *
     * @param resource 资源对象
     * @return 资源大小可选值
     */
    public static OptionalLong tryContentLength(Resource resource) {
        if (resource == null) {
            return OptionalLong.empty();
        }
        try {
            return OptionalLong.of(resource.contentLength());
        } catch (IOException | RuntimeException ex) {
            return OptionalLong.empty();
        }
    }

    /**
     * 安全获取资源最后修改时间。
     *
     * @param resource 资源对象
     * @return 最后修改时间可选值
     */
    public static OptionalLong tryLastModified(Resource resource) {
        if (resource == null) {
            return OptionalLong.empty();
        }
        try {
            return OptionalLong.of(resource.lastModified());
        } catch (IOException | RuntimeException ex) {
            return OptionalLong.empty();
        }
    }

    /**
     * 读取字节并将受检异常包装为运行时异常。
     *
     * @param resource 资源对象
     * @return 字节数组
     */
    public static byte[] sneakyReadBytes(Resource resource) {
        return readBytes(resource);
    }

    /**
     * 包装资源操作异常。
     *
     * @param exception 原始异常
     * @return 资源操作异常
     */
    public static ResourceOperationException wrapException(Exception exception) {
        Objects.requireNonNull(exception, "异常不能为空");
        if (exception instanceof ResourceOperationException resourceException) {
            return resourceException;
        }
        if (exception instanceof IOException ioException) {
            return new ResourceOperationException("资源 IO 操作失败", ioException);
        }
        return new ResourceOperationException("资源操作失败", exception);
    }

    /**
     * 创建资源不存在异常。
     *
     * @param location 资源定位字符串
     * @return 资源操作异常
     */
    public static ResourceOperationException resourceNotFound(String location) {
        return new ResourceOperationException("资源不存在: " + location);
    }

    /**
     * 创建资源不可读异常。
     *
     * @param resource 资源对象
     * @return 资源操作异常
     */
    public static ResourceOperationException resourceNotReadable(Resource resource) {
        return new ResourceOperationException("资源不可读: " + getReadableDescription(resource));
    }

    /**
     * 资源校验器。
     *
     * @author Ateng
     * @since 2026-04-30
     */
    @FunctionalInterface
    public interface ResourceValidator {

        /**
         * 校验资源。
         *
         * @param resource 资源对象
         */
        void validate(Resource resource);
    }

    /**
     * 资源操作异常。
     *
     * @author Ateng
     * @since 2026-04-30
     */
    public static class ResourceOperationException extends RuntimeException {

        /**
         * 创建资源操作异常。
         *
         * @param message 异常消息
         */
        public ResourceOperationException(String message) {
            super(message);
        }

        /**
         * 创建资源操作异常。
         *
         * @param message 异常消息
         * @param cause   原始异常
         */
        public ResourceOperationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * 资源分片描述。
     *
     * @author Ateng
     * @since 2026-04-30
     */
    public static final class ResourceRegion {
        private final Resource resource;
        private final long position;
        private final long count;

        private ResourceRegion(Resource resource, long position, long count) {
            this.resource = resource;
            this.position = position;
            this.count = count;
        }

        /**
         * 获取资源对象。
         *
         * @return 资源对象
         */
        public Resource getResource() {
            return resource;
        }

        /**
         * 获取分片起始位置。
         *
         * @return 起始位置
         */
        public long getPosition() {
            return position;
        }

        /**
         * 获取分片长度。
         *
         * @return 分片长度
         */
        public long getCount() {
            return count;
        }
    }

    /**
     * 资源 HTTP 响应描述。
     *
     * @author Ateng
     * @since 2026-04-30
     */
    public static final class ResourceHttpResponse {
        private final Resource resource;
        private final int status;
        private final Map<String, String> headers;
        private final String contentType;

        private ResourceHttpResponse(Resource resource, int status, Map<String, String> headers, String contentType) {
            this.resource = resource;
            this.status = status;
            this.headers = Collections.unmodifiableMap(new LinkedHashMap<>(headers));
            this.contentType = contentType;
        }

        /**
         * 获取资源对象。
         *
         * @return 资源对象
         */
        public Resource getResource() {
            return resource;
        }

        /**
         * 获取响应状态码。
         *
         * @return 状态码
         */
        public int getStatus() {
            return status;
        }

        /**
         * 获取响应头。
         *
         * @return 响应头
         */
        public Map<String, String> getHeaders() {
            return headers;
        }

        /**
         * 获取内容类型。
         *
         * @return 内容类型
         */
        public String getContentType() {
            return contentType;
        }
    }

    /**
     * 资源表单文件包装对象。
     *
     * @author Ateng
     * @since 2026-04-30
     */
    public static final class ResourceMultipartFile {
        private final Resource resource;

        private ResourceMultipartFile(Resource resource) {
            this.resource = resource;
        }

        /**
         * 获取文件名。
         *
         * @return 文件名
         */
        public String getName() {
            return ResourceUtil.getFilename(resource, "resource");
        }

        /**
         * 获取原始文件名。
         *
         * @return 原始文件名
         */
        public String getOriginalFilename() {
            return getName();
        }

        /**
         * 获取内容类型。
         *
         * @return 内容类型
         */
        public String getContentType() {
            return ResourceUtil.getContentType(resource);
        }

        /**
         * 判断文件是否为空。
         *
         * @return 为空返回 true
         */
        public boolean isEmpty() {
            return ResourceUtil.isEmpty(resource);
        }

        /**
         * 获取文件大小。
         *
         * @return 文件大小
         */
        public long getSize() {
            return ResourceUtil.getContentLength(resource);
        }

        /**
         * 获取文件字节数组。
         *
         * @return 字节数组
         */
        public byte[] getBytes() {
            return ResourceUtil.readBytes(resource);
        }

        /**
         * 获取输入流。
         *
         * @return 输入流
         */
        public InputStream getInputStream() {
            return ResourceUtil.getInputStream(resource);
        }

        /**
         * 将文件传输到目标路径。
         *
         * @param destination 目标路径
         */
        public void transferTo(Path destination) {
            ResourceUtil.copy(resource, destination);
        }

        /**
         * 获取底层资源对象。
         *
         * @return 资源对象
         */
        public Resource getResource() {
            return resource;
        }
    }

    private static <T> T readTextObject(Resource resource, Function<String, T> converter) {
        Objects.requireNonNull(converter, "文本转换器不能为空");
        return converter.apply(readString(resource, DEFAULT_CHARSET));
    }

    private static void requireText(String text, String message) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    private static void createParentDirectories(Path target) {
        Path parent = target.toAbsolutePath().getParent();
        if (parent == null) {
            return;
        }
        try {
            Files.createDirectories(parent);
        } catch (IOException ex) {
            throw new ResourceOperationException("创建父目录失败: " + parent, ex);
        }
    }

    private static String normalizeExtension(String extension) {
        requireText(extension, "扩展名不能为空");
        return extension.startsWith(".") ? extension.substring(1).toLowerCase(Locale.ROOT) : extension.toLowerCase(Locale.ROOT);
    }

    private static Set<String> normalizeExtensions(String... extensions) {
        Set<String> result = new HashSet<>();
        for (String extension : extensions) {
            if (extension != null && !extension.isBlank()) {
                result.add(normalizeExtension(extension));
            }
        }
        return result;
    }

    private static String manualContentType(String extension) {
        if (extension == null || extension.isBlank()) {
            return DEFAULT_BINARY_CONTENT_TYPE;
        }
        return switch (extension.toLowerCase(Locale.ROOT)) {
            case "txt", "log" -> "text/plain";
            case "html", "htm" -> "text/html";
            case "css" -> "text/css";
            case "csv" -> "text/csv";
            case "json" -> "application/json";
            case "xml" -> "application/xml";
            case "yaml", "yml" -> "application/yaml";
            case "properties" -> "text/x-java-properties";
            case "sql" -> "application/sql";
            case "pdf" -> "application/pdf";
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "gif" -> "image/gif";
            case "svg" -> "image/svg+xml";
            case "zip" -> "application/zip";
            default -> TEXT_EXTENSIONS.contains(extension) ? "text/plain" : DEFAULT_BINARY_CONTENT_TYPE;
        };
    }

    private static String trimSlashes(String value) {
        int start = 0;
        int end = value.length();
        while (start < end && value.charAt(start) == '/') {
            start++;
        }
        while (end > start && value.charAt(end - 1) == '/') {
            end--;
        }
        return value.substring(start, end);
    }

    private static String cacheKey(Resource resource) {
        return tryGetUri(resource).map(URI::toString).orElseGet(() -> getReadableDescription(resource));
    }

    private static final class NamedByteArrayResource extends ByteArrayResource {
        private final String filename;

        private NamedByteArrayResource(byte[] byteArray, String filename, String description) {
            super(byteArray, description);
            this.filename = filename;
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }

    private static final class SupplierInputStreamResource extends AbstractResource {
        private final Supplier<InputStream> supplier;
        private final String filename;

        private SupplierInputStreamResource(Supplier<InputStream> supplier, String filename) {
            this.supplier = supplier;
            this.filename = filename;
        }

        @Override
        public String getDescription() {
            return filename == null ? "supplier input stream resource" : "supplier input stream resource: " + filename;
        }

        @Override
        public String getFilename() {
            return filename;
        }

        @Override
        public boolean exists() {
            return true;
        }

        @Override
        public boolean isReadable() {
            return true;
        }

        @Override
        public InputStream getInputStream() {
            InputStream inputStream = supplier.get();
            if (inputStream == null) {
                throw new ResourceOperationException("输入流供应器返回了空值");
            }
            return inputStream;
        }

        @Override
        public long contentLength() throws IOException {
            try (InputStream inputStream = getInputStream()) {
                return inputStream.transferTo(OutputStream.nullOutputStream());
            }
        }
    }
}
