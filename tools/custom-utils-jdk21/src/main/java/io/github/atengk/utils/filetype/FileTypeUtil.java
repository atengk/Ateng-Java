package io.github.atengk.utils.filetype;

import org.apache.tika.Tika;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.exception.TikaException;
import org.apache.tika.mime.MediaType;
import org.xml.sax.SAXException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于 Apache Tika 的文件类型识别工具类。
 *
 * @author Ateng
 * @since 2026-04-29
 */
public final class FileTypeUtil {

    private static final String DEFAULT_MIME_TYPE = "application/octet-stream";
    private static final Tika DEFAULT_TIKA = new Tika();
    private static final Map<String, String> EXTENSION_TO_MIME = buildExtensionToMimeMap();
    private static final Map<String, Set<String>> MIME_TO_EXTENSIONS = buildMimeToExtensionsMap(EXTENSION_TO_MIME);
    private static final Map<String, String> CUSTOM_EXTENSION_TO_MIME = new ConcurrentHashMap<>();
    private static final Map<String, String> CUSTOM_MIME_TO_EXTENSION = new ConcurrentHashMap<>();

    private static final Set<String> CODE_EXTENSIONS = Set.of(
            "java", "kt", "kts", "groovy", "scala", "js", "jsx", "ts", "tsx", "vue", "html", "htm", "css", "scss", "sass",
            "less", "xml", "json", "yaml", "yml", "properties", "sql", "py", "go", "rs", "c", "h", "cpp", "hpp", "cs", "php",
            "rb", "swift", "lua", "r", "dart", "sh", "bat", "cmd", "ps1", "md", "gradle", "dockerfile"
    );

    private static final Set<String> SCRIPT_EXTENSIONS = Set.of(
            "sh", "bash", "zsh", "fish", "bat", "cmd", "ps1", "psm1", "vbs", "vbe", "js", "jse", "wsf", "wsh", "hta", "py", "rb", "php", "pl", "lua"
    );

    private static final Set<String> EXECUTABLE_EXTENSIONS = Set.of(
            "exe", "msi", "dll", "com", "scr", "sys", "app", "deb", "rpm", "dmg", "pkg", "run", "bin", "jar", "war", "ear", "so"
    );

    private static final Set<String> DATABASE_EXTENSIONS = Set.of(
            "db", "sqlite", "sqlite3", "mdb", "accdb", "frm", "ibd"
    );

    private static final Set<String> FONT_EXTENSIONS = Set.of(
            "ttf", "otf", "woff", "woff2", "eot"
    );

    private static final Set<String> EMAIL_EXTENSIONS = Set.of(
            "eml", "msg", "mht", "mhtml"
    );

    private static final Set<String> DANGEROUS_EXTENSIONS = Set.of(
            "exe", "bat", "cmd", "sh", "bash", "zsh", "ps1", "psm1", "vbs", "vbe", "js", "jse", "wsf", "wsh", "jar", "war", "ear",
            "dll", "so", "msi", "scr", "com", "hta", "php", "pl", "py", "rb", "lua", "run", "bin", "sys"
    );

    private static final Set<String> DANGEROUS_MIME_TYPES = Set.of(
            "application/x-msdownload",
            "application/x-msdos-program",
            "application/x-ms-installer",
            "application/x-sh",
            "application/x-shellscript",
            "application/x-bat",
            "application/x-csh",
            "application/x-executable",
            "application/x-dosexec",
            "application/java-archive",
            "application/x-java-archive",
            "application/x-sharedlib",
            "application/vnd.microsoft.portable-executable",
            "text/x-php",
            "text/x-python",
            "text/x-perl",
            "text/javascript",
            "application/javascript",
            "application/x-javascript"
    );

    private FileTypeUtil() {
        throw new UnsupportedOperationException("FileTypeUtil 是静态工具类，禁止实例化");
    }

    /**
     * 根据文件路径识别 MIME 类型。
     *
     * @param path 文件路径
     * @return MIME 类型
     */
    public static String detectMimeType(Path path) {
        requireExistingFile(path);
        try {
            return normalizeMimeType(DEFAULT_TIKA.detect(path));
        } catch (IOException e) {
            throw new FileTypeException("识别文件 MIME 类型失败: " + path, e);
        }
    }

    /**
     * 根据 File 对象识别 MIME 类型。
     *
     * @param file 文件对象
     * @return MIME 类型
     */
    public static String detectMimeType(File file) {
        if (file == null) {
            throw new IllegalArgumentException("文件对象不能为空");
        }
        return detectMimeType(file.toPath());
    }

    /**
     * 根据输入流识别 MIME 类型，不会主动关闭输入流。
     *
     * @param inputStream 输入流
     * @return MIME 类型
     */
    public static String detectMimeType(InputStream inputStream) {
        InputStream stream = requireInputStream(inputStream);
        try {
            return normalizeMimeType(DEFAULT_TIKA.detect(ensureBuffered(stream)));
        } catch (IOException e) {
            throw new FileTypeException("根据输入流识别 MIME 类型失败", e);
        }
    }

    /**
     * 根据输入流和文件名识别 MIME 类型，不会主动关闭输入流。
     *
     * @param inputStream 输入流
     * @param fileName 文件名
     * @return MIME 类型
     */
    public static String detectMimeType(InputStream inputStream, String fileName) {
        InputStream stream = requireInputStream(inputStream);
        try {
            return normalizeMimeType(DEFAULT_TIKA.detect(ensureBuffered(stream), nullToEmpty(fileName)));
        } catch (IOException e) {
            throw new FileTypeException("根据输入流和文件名识别 MIME 类型失败: " + nullToEmpty(fileName), e);
        }
    }

    /**
     * 根据字节数组识别 MIME 类型。
     *
     * @param bytes 文件字节数组
     * @return MIME 类型
     */
    public static String detectMimeType(byte[] bytes) {
        if (bytes == null) {
            throw new IllegalArgumentException("文件字节数组不能为空");
        }
        return normalizeMimeType(DEFAULT_TIKA.detect(bytes));
    }

    /**
     * 根据文件路径识别 Tika MediaType。
     *
     * @param path 文件路径
     * @return MediaType
     */
    public static MediaType detectMediaType(Path path) {
        return parseMediaType(detectMimeType(path));
    }

    /**
     * 根据文件路径识别完整文件类型信息。
     *
     * @param path 文件路径
     * @return 文件类型信息
     */
    public static FileTypeInfo detectFileType(Path path) {
        return detectInfo(path);
    }

    /**
     * 根据输入流和文件名识别完整文件类型信息。
     *
     * @param inputStream 输入流
     * @param fileName 文件名
     * @return 文件类型信息
     */
    public static FileTypeInfo detectFileType(InputStream inputStream, String fileName) {
        return detectInfo(inputStream, fileName);
    }

    /**
     * 获取文件名中的扩展名，不包含点号。
     *
     * @param fileName 文件名
     * @return 扩展名，不存在时返回空字符串
     */
    public static String getExtension(String fileName) {
        String safeName = nullToEmpty(fileName).trim();
        if (safeName.isEmpty()) {
            return "";
        }
        int separatorIndex = Math.max(safeName.lastIndexOf('/'), safeName.lastIndexOf('\\'));
        String simpleName = separatorIndex >= 0 ? safeName.substring(separatorIndex + 1) : safeName;
        int index = simpleName.lastIndexOf('.');
        if (index <= 0 || index == simpleName.length() - 1) {
            return "";
        }
        return normalizeExtension(simpleName.substring(index + 1));
    }

    /**
     * 获取路径中的扩展名，不包含点号。
     *
     * @param path 文件路径
     * @return 扩展名，不存在时返回空字符串
     */
    public static String getExtension(Path path) {
        if (path == null || path.getFileName() == null) {
            return "";
        }
        return getExtension(path.getFileName().toString());
    }

    /**
     * 规范化扩展名。
     *
     * @param extension 扩展名
     * @return 规范化后的扩展名
     */
    public static String normalizeExtension(String extension) {
        String value = nullToEmpty(extension).trim().toLowerCase(Locale.ROOT);
        while (value.startsWith(".")) {
            value = value.substring(1);
        }
        return value;
    }

    /**
     * 判断文件名是否包含扩展名。
     *
     * @param fileName 文件名
     * @return true 表示包含扩展名
     */
    public static boolean hasExtension(String fileName) {
        return !getExtension(fileName).isEmpty();
    }

    /**
     * 获取不带扩展名的文件名。
     *
     * @param fileName 文件名
     * @return 不带扩展名的文件名
     */
    public static String getFileNameWithoutExtension(String fileName) {
        String safeName = nullToEmpty(fileName).trim();
        if (safeName.isEmpty()) {
            return "";
        }
        int separatorIndex = Math.max(safeName.lastIndexOf('/'), safeName.lastIndexOf('\\'));
        String parent = separatorIndex >= 0 ? safeName.substring(0, separatorIndex + 1) : "";
        String simpleName = separatorIndex >= 0 ? safeName.substring(separatorIndex + 1) : safeName;
        int index = simpleName.lastIndexOf('.');
        if (index <= 0) {
            return safeName;
        }
        return parent + simpleName.substring(0, index);
    }

    /**
     * 根据扩展名推断 MIME 类型。
     *
     * @param extension 扩展名
     * @return MIME 类型，无法识别时返回 application/octet-stream
     */
    public static String getMimeTypeByExtension(String extension) {
        String ext = normalizeExtension(extension);
        if (ext.isEmpty()) {
            return DEFAULT_MIME_TYPE;
        }
        String custom = CUSTOM_EXTENSION_TO_MIME.get(ext);
        if (custom != null) {
            return custom;
        }
        String mapped = EXTENSION_TO_MIME.get(ext);
        if (mapped != null) {
            return mapped;
        }
        return normalizeMimeType(DEFAULT_TIKA.detect("file." + ext));
    }

    /**
     * 根据 MIME 类型获取推荐扩展名。
     *
     * @param mimeType MIME 类型
     * @return 推荐扩展名，不存在时返回空字符串
     */
    public static String getExtensionByMimeType(String mimeType) {
        String normalized = normalizeMimeType(mimeType);
        if (normalized.isEmpty()) {
            return "";
        }
        String custom = CUSTOM_MIME_TO_EXTENSION.get(normalized);
        if (custom != null) {
            return custom;
        }
        Set<String> extensions = MIME_TO_EXTENSIONS.get(normalized);
        if (extensions == null || extensions.isEmpty()) {
            return "";
        }
        return extensions.iterator().next();
    }

    /**
     * 判断文件名或扩展名是否匹配 MIME 类型。
     *
     * @param fileNameOrExtension 文件名或扩展名
     * @param mimeType MIME 类型
     * @return true 表示匹配
     */
    public static boolean isExtensionMatchedMimeType(String fileNameOrExtension, String mimeType) {
        String extension = extractExtensionOrSelf(fileNameOrExtension);
        return isExtensionMatchedMimeTypeByExtension(extension, mimeType);
    }

    /**
     * 判断指定扩展名是否匹配 MIME 类型。
     *
     * @param extension 扩展名
     * @param mimeType MIME 类型
     * @return true 表示匹配
     */
    public static boolean isExtensionMatchedMimeTypeByExtension(String extension, String mimeType) {
        String ext = normalizeExtension(extension);
        String normalizedMime = normalizeMimeType(mimeType);
        if (ext.isEmpty() || normalizedMime.isEmpty()) {
            return false;
        }
        if (Objects.equals(getMimeTypeByExtension(ext), normalizedMime)) {
            return true;
        }
        Set<String> candidates = new LinkedHashSet<>();
        Set<String> mapped = MIME_TO_EXTENSIONS.get(normalizedMime);
        if (mapped != null) {
            candidates.addAll(mapped);
        }
        String custom = CUSTOM_MIME_TO_EXTENSION.get(normalizedMime);
        if (custom != null) {
            candidates.add(custom);
        }
        return candidates.contains(ext);
    }

    /**
     * 判断文件名是否存在双扩展名。
     *
     * @param fileName 文件名
     * @return true 表示存在双扩展名
     */
    public static boolean isDoubleExtension(String fileName) {
        return getAllExtensions(fileName).size() >= 2;
    }

    /**
     * 获取文件名最后一级扩展名。
     *
     * @param fileName 文件名
     * @return 最后一级扩展名
     */
    public static String getLastExtension(String fileName) {
        return getExtension(fileName);
    }

    /**
     * 获取文件名中所有扩展名。
     *
     * @param fileName 文件名
     * @return 扩展名列表
     */
    public static List<String> getAllExtensions(String fileName) {
        String safeName = nullToEmpty(fileName).trim();
        if (safeName.isEmpty()) {
            return List.of();
        }
        int separatorIndex = Math.max(safeName.lastIndexOf('/'), safeName.lastIndexOf('\\'));
        String simpleName = separatorIndex >= 0 ? safeName.substring(separatorIndex + 1) : safeName;
        if (simpleName.startsWith(".") && simpleName.indexOf('.', 1) < 0) {
            return List.of();
        }
        String[] parts = simpleName.split("\\.");
        if (parts.length <= 1) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (int i = 1; i < parts.length; i++) {
            String ext = normalizeExtension(parts[i]);
            if (!ext.isEmpty()) {
                result.add(ext);
            }
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * 获取 MIME 主类型。
     *
     * @param mimeType MIME 类型
     * @return 主类型，不存在时返回空字符串
     */
    public static String getMajorType(String mimeType) {
        String normalized = normalizeMimeType(mimeType);
        int index = normalized.indexOf('/');
        return index > 0 ? normalized.substring(0, index) : "";
    }

    /**
     * 获取 MIME 子类型。
     *
     * @param mimeType MIME 类型
     * @return 子类型，不存在时返回空字符串
     */
    public static String getSubType(String mimeType) {
        String normalized = normalizeMimeType(mimeType);
        int index = normalized.indexOf('/');
        return index > 0 && index < normalized.length() - 1 ? normalized.substring(index + 1) : "";
    }

    /**
     * 规范化 MIME 类型。
     *
     * @param mimeType MIME 类型
     * @return 规范化后的 MIME 类型
     */
    public static String normalizeMimeType(String mimeType) {
        String value = nullToEmpty(mimeType).trim().toLowerCase(Locale.ROOT);
        int semicolonIndex = value.indexOf(';');
        if (semicolonIndex >= 0) {
            value = value.substring(0, semicolonIndex).trim();
        }
        return value;
    }

    /**
     * 判断两个 MIME 类型是否一致。
     *
     * @param mimeType 实际 MIME 类型
     * @param expectedMimeType 期望 MIME 类型
     * @return true 表示一致
     */
    public static boolean isMimeType(String mimeType, String expectedMimeType) {
        String actual = normalizeMimeType(mimeType);
        String expected = normalizeMimeType(expectedMimeType);
        return !actual.isEmpty() && actual.equals(expected);
    }

    /**
     * 判断 MIME 类型是否匹配指定模式，支持主类型通配符和全量通配符。
     *
     * @param mimeType MIME 类型
     * @param pattern 匹配模式
     * @return true 表示匹配
     */
    public static boolean isMimeTypeMatched(String mimeType, String pattern) {
        String actual = normalizeMimeType(mimeType);
        String expected = normalizeMimeType(pattern);
        if (actual.isEmpty() || expected.isEmpty()) {
            return false;
        }
        if ("*/*".equals(expected) || "*".equals(expected)) {
            return true;
        }
        if (expected.endsWith("/*")) {
            String expectedMajor = expected.substring(0, expected.length() - 2);
            return expectedMajor.equals(getMajorType(actual));
        }
        return actual.equals(expected);
    }

    /**
     * 判断 MIME 类型是否匹配任意模式。
     *
     * @param mimeType MIME 类型
     * @param patterns 匹配模式集合
     * @return true 表示匹配任意模式
     */
    public static boolean isAnyMimeTypeMatched(String mimeType, Collection<String> patterns) {
        if (patterns == null || patterns.isEmpty()) {
            return false;
        }
        for (String pattern : patterns) {
            if (isMimeTypeMatched(mimeType, pattern)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断是否为已知 MIME 类型。
     *
     * @param mimeType MIME 类型
     * @return true 表示已知类型
     */
    public static boolean isKnownMimeType(String mimeType) {
        return !isUnknownMimeType(mimeType);
    }

    /**
     * 判断是否为未知 MIME 类型。
     *
     * @param mimeType MIME 类型
     * @return true 表示未知类型
     */
    public static boolean isUnknownMimeType(String mimeType) {
        String normalized = normalizeMimeType(mimeType);
        return normalized.isEmpty() || DEFAULT_MIME_TYPE.equals(normalized);
    }

    /**
     * 判断是否为 application 主类型。
     *
     * @param mimeType MIME 类型
     * @return true 表示 application 类型
     */
    public static boolean isApplicationType(String mimeType) {
        return "application".equals(getMajorType(mimeType));
    }

    /**
     * 判断是否为 text 主类型。
     *
     * @param mimeType MIME 类型
     * @return true 表示 text 类型
     */
    public static boolean isTextType(String mimeType) {
        return "text".equals(getMajorType(mimeType));
    }

    /**
     * 判断是否倾向于二进制类型。
     *
     * @param mimeType MIME 类型
     * @return true 表示二进制类型
     */
    public static boolean isBinaryType(String mimeType) {
        String normalized = normalizeMimeType(mimeType);
        if (normalized.isEmpty()) {
            return false;
        }
        return !(isTextType(normalized) || isJson(normalized, "") || isXml(normalized, "") || isCsv(normalized, ""));
    }

    /**
     * 判断是否图片类型。
     *
     * @param mimeType MIME 类型
     * @return true 表示图片类型
     */
    public static boolean isImage(String mimeType) {
        return "image".equals(getMajorType(mimeType));
    }

    /**
     * 判断是否音频类型。
     *
     * @param mimeType MIME 类型
     * @return true 表示音频类型
     */
    public static boolean isAudio(String mimeType) {
        return "audio".equals(getMajorType(mimeType));
    }

    /**
     * 判断是否视频类型。
     *
     * @param mimeType MIME 类型
     * @return true 表示视频类型
     */
    public static boolean isVideo(String mimeType) {
        return "video".equals(getMajorType(mimeType));
    }

    /**
     * 判断是否文本类型。
     *
     * @param mimeType MIME 类型
     * @return true 表示文本类型
     */
    public static boolean isText(String mimeType) {
        return isTextType(mimeType);
    }

    /**
     * 判断是否 PDF 类型。
     *
     * @param mimeType MIME 类型
     * @return true 表示 PDF 类型
     */
    public static boolean isPdf(String mimeType) {
        return isMimeType(mimeType, "application/pdf");
    }

    /**
     * 判断是否 Office 文档类型。
     *
     * @param mimeType MIME 类型
     * @return true 表示 Office 类型
     */
    public static boolean isOffice(String mimeType) {
        return isWord(mimeType) || isExcel(mimeType) || isPowerPoint(mimeType);
    }

    /**
     * 判断是否 Word 文档类型。
     *
     * @param mimeType MIME 类型
     * @return true 表示 Word 类型
     */
    public static boolean isWord(String mimeType) {
        String normalized = normalizeMimeType(mimeType);
        return Set.of(
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/vnd.ms-word.document.macroenabled.12"
        ).contains(normalized);
    }

    /**
     * 判断是否 Excel 表格类型。
     *
     * @param mimeType MIME 类型
     * @return true 表示 Excel 类型
     */
    public static boolean isExcel(String mimeType) {
        String normalized = normalizeMimeType(mimeType);
        return Set.of(
                "application/vnd.ms-excel",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "application/vnd.ms-excel.sheet.macroenabled.12",
                "application/vnd.oasis.opendocument.spreadsheet"
        ).contains(normalized);
    }

    /**
     * 判断是否 PowerPoint 演示文稿类型。
     *
     * @param mimeType MIME 类型
     * @return true 表示 PowerPoint 类型
     */
    public static boolean isPowerPoint(String mimeType) {
        String normalized = normalizeMimeType(mimeType);
        return Set.of(
                "application/vnd.ms-powerpoint",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                "application/vnd.ms-powerpoint.presentation.macroenabled.12",
                "application/vnd.oasis.opendocument.presentation"
        ).contains(normalized);
    }

    /**
     * 判断是否压缩包类型。
     *
     * @param mimeType MIME 类型
     * @return true 表示压缩包类型
     */
    public static boolean isArchive(String mimeType) {
        String normalized = normalizeMimeType(mimeType);
        return Set.of(
                "application/zip",
                "application/x-zip-compressed",
                "application/x-7z-compressed",
                "application/x-rar-compressed",
                "application/vnd.rar",
                "application/gzip",
                "application/x-gzip",
                "application/x-tar",
                "application/x-bzip2",
                "application/x-xz",
                "application/java-archive"
        ).contains(normalized);
    }

    /**
     * 判断是否代码文件。
     *
     * @param mimeType MIME 类型
     * @param extension 扩展名
     * @return true 表示代码文件
     */
    public static boolean isCode(String mimeType, String extension) {
        String ext = normalizeExtension(extension);
        String normalized = normalizeMimeType(mimeType);
        return CODE_EXTENSIONS.contains(ext)
                || normalized.startsWith("text/x-")
                || normalized.startsWith("application/x-") && CODE_EXTENSIONS.contains(ext);
    }

    /**
     * 判断是否 JSON 文件。
     *
     * @param mimeType MIME 类型
     * @param extension 扩展名
     * @return true 表示 JSON 文件
     */
    public static boolean isJson(String mimeType, String extension) {
        String normalized = normalizeMimeType(mimeType);
        return "json".equals(normalizeExtension(extension))
                || "application/json".equals(normalized)
                || normalized.endsWith("+json");
    }

    /**
     * 判断是否 XML 文件。
     *
     * @param mimeType MIME 类型
     * @param extension 扩展名
     * @return true 表示 XML 文件
     */
    public static boolean isXml(String mimeType, String extension) {
        String normalized = normalizeMimeType(mimeType);
        return "xml".equals(normalizeExtension(extension))
                || "application/xml".equals(normalized)
                || "text/xml".equals(normalized)
                || normalized.endsWith("+xml");
    }

    /**
     * 判断是否 CSV 文件。
     *
     * @param mimeType MIME 类型
     * @param extension 扩展名
     * @return true 表示 CSV 文件
     */
    public static boolean isCsv(String mimeType, String extension) {
        String normalized = normalizeMimeType(mimeType);
        return "csv".equals(normalizeExtension(extension))
                || "text/csv".equals(normalized)
                || "application/csv".equals(normalized);
    }

    /**
     * 判断是否 Markdown 文件。
     *
     * @param mimeType MIME 类型
     * @param extension 扩展名
     * @return true 表示 Markdown 文件
     */
    public static boolean isMarkdown(String mimeType, String extension) {
        String ext = normalizeExtension(extension);
        String normalized = normalizeMimeType(mimeType);
        return "md".equals(ext) || "markdown".equals(ext)
                || "text/markdown".equals(normalized)
                || "text/x-markdown".equals(normalized);
    }

    /**
     * 判断是否字体文件。
     *
     * @param mimeType MIME 类型
     * @return true 表示字体文件
     */
    public static boolean isFont(String mimeType) {
        String normalized = normalizeMimeType(mimeType);
        return normalized.startsWith("font/")
                || normalized.startsWith("application/font-")
                || Set.of("application/vnd.ms-fontobject", "application/x-font-ttf", "application/x-font-otf", "application/x-font-woff").contains(normalized);
    }

    /**
     * 判断是否邮件文件。
     *
     * @param mimeType MIME 类型
     * @return true 表示邮件文件
     */
    public static boolean isEmail(String mimeType) {
        String normalized = normalizeMimeType(mimeType);
        return Set.of("message/rfc822", "application/vnd.ms-outlook", "multipart/related").contains(normalized);
    }

    /**
     * 判断是否可执行文件。
     *
     * @param mimeType MIME 类型
     * @param extension 扩展名
     * @return true 表示可执行文件
     */
    public static boolean isExecutable(String mimeType, String extension) {
        return isExecutableFile(mimeType, extension);
    }

    /**
     * 判断是否脚本文件。
     *
     * @param mimeType MIME 类型
     * @param extension 扩展名
     * @return true 表示脚本文件
     */
    public static boolean isScript(String mimeType, String extension) {
        return isScriptFile(mimeType, extension);
    }

    /**
     * 判断是否数据库文件。
     *
     * @param mimeType MIME 类型
     * @param extension 扩展名
     * @return true 表示数据库文件
     */
    public static boolean isDatabaseFile(String mimeType, String extension) {
        String ext = normalizeExtension(extension);
        String normalized = normalizeMimeType(mimeType);
        return DATABASE_EXTENSIONS.contains(ext)
                || Set.of("application/vnd.sqlite3", "application/x-sqlite3", "application/x-msaccess").contains(normalized);
    }

    /**
     * 根据 MIME 类型获取文件分类。
     *
     * @param mimeType MIME 类型
     * @return 文件分类
     */
    public static FileTypeCategory getCategory(String mimeType) {
        return getCategory(mimeType, "");
    }

    /**
     * 根据 MIME 类型和扩展名获取文件分类。
     *
     * @param mimeType MIME 类型
     * @param extension 扩展名
     * @return 文件分类
     */
    public static FileTypeCategory getCategory(String mimeType, String extension) {
        String ext = normalizeExtension(extension);
        if (isExecutableFile(mimeType, ext)) {
            return FileTypeCategory.EXECUTABLE;
        }
        if (isScriptFile(mimeType, ext)) {
            return FileTypeCategory.SCRIPT;
        }
        if (isImage(mimeType)) {
            return FileTypeCategory.IMAGE;
        }
        if (isAudio(mimeType)) {
            return FileTypeCategory.AUDIO;
        }
        if (isVideo(mimeType)) {
            return FileTypeCategory.VIDEO;
        }
        if (isPdf(mimeType)) {
            return FileTypeCategory.PDF;
        }
        if (isWord(mimeType)) {
            return FileTypeCategory.WORD;
        }
        if (isExcel(mimeType)) {
            return FileTypeCategory.EXCEL;
        }
        if (isPowerPoint(mimeType)) {
            return FileTypeCategory.POWERPOINT;
        }
        if (isArchive(mimeType)) {
            return FileTypeCategory.ARCHIVE;
        }
        if (isFont(mimeType) || FONT_EXTENSIONS.contains(ext)) {
            return FileTypeCategory.FONT;
        }
        if (isEmail(mimeType) || EMAIL_EXTENSIONS.contains(ext)) {
            return FileTypeCategory.EMAIL;
        }
        if (isDatabaseFile(mimeType, ext)) {
            return FileTypeCategory.DATABASE;
        }
        if (isJson(mimeType, ext) || isXml(mimeType, ext) || isCsv(mimeType, ext) || isMarkdown(mimeType, ext) || isCode(mimeType, ext)) {
            return FileTypeCategory.CODE;
        }
        if (isText(mimeType)) {
            return FileTypeCategory.TEXT;
        }
        if (isApplicationType(mimeType) && isKnownMimeType(mimeType)) {
            return FileTypeCategory.DOCUMENT;
        }
        return FileTypeCategory.UNKNOWN;
    }

    /**
     * 根据文件路径识别文件分类。
     *
     * @param path 文件路径
     * @return 文件分类
     */
    public static FileTypeCategory getCategory(Path path) {
        FileTypeInfo info = detectInfo(path);
        return info.getCategory();
    }

    /**
     * 根据 MIME 类型获取分类名称。
     *
     * @param mimeType MIME 类型
     * @return 分类名称
     */
    public static String getCategoryName(String mimeType) {
        return getCategory(mimeType).getDisplayName();
    }

    /**
     * 判断 MIME 类型是否属于指定分类。
     *
     * @param mimeType MIME 类型
     * @param category 文件分类
     * @return true 表示属于指定分类
     */
    public static boolean isCategory(String mimeType, FileTypeCategory category) {
        return category != null && getCategory(mimeType) == category;
    }

    /**
     * 判断 MIME 类型是否属于任意指定分类。
     *
     * @param mimeType MIME 类型
     * @param categories 文件分类集合
     * @return true 表示属于任意指定分类
     */
    public static boolean isAnyCategory(String mimeType, Collection<FileTypeCategory> categories) {
        if (categories == null || categories.isEmpty()) {
            return false;
        }
        return categories.contains(getCategory(mimeType));
    }

    /**
     * 校验上传文件类型并返回校验结果。
     *
     * @param inputStream 输入流
     * @param fileName 文件名
     * @param policy 校验策略
     * @return 校验结果
     */
    public static FileTypeCheckResult checkUploadType(InputStream inputStream, String fileName, FileTypePolicy policy) {
        FileTypePolicy actualPolicy = policy == null ? FileTypePolicy.defaults() : policy;
        FileTypeInfo info = detectInfo(inputStream, fileName);
        List<String> messages = validateInfo(info, actualPolicy);
        return messages.isEmpty() ? FileTypeCheckResult.passed(info) : FileTypeCheckResult.failed(info, messages);
    }

    /**
     * 校验上传文件类型，校验失败时抛出异常。
     *
     * @param inputStream 输入流
     * @param fileName 文件名
     * @param policy 校验策略
     */
    public static void assertUploadType(InputStream inputStream, String fileName, FileTypePolicy policy) {
        FileTypeCheckResult result = checkUploadType(inputStream, fileName, policy);
        if (!result.isPassed()) {
            throw new UnsupportedFileTypeException(String.join("; ", result.getMessages()));
        }
    }

    /**
     * 判断 MIME 类型是否在允许集合中。
     *
     * @param mimeType MIME 类型
     * @param allowMimeTypes 允许集合
     * @return true 表示允许
     */
    public static boolean isAllowedMimeType(String mimeType, Collection<String> allowMimeTypes) {
        if (allowMimeTypes == null || allowMimeTypes.isEmpty()) {
            return true;
        }
        return isAnyMimeTypeMatched(mimeType, allowMimeTypes);
    }

    /**
     * 判断 MIME 类型是否在禁止集合中。
     *
     * @param mimeType MIME 类型
     * @param denyMimeTypes 禁止集合
     * @return true 表示禁止
     */
    public static boolean isDeniedMimeType(String mimeType, Collection<String> denyMimeTypes) {
        if (denyMimeTypes == null || denyMimeTypes.isEmpty()) {
            return false;
        }
        return isAnyMimeTypeMatched(mimeType, denyMimeTypes);
    }

    /**
     * 判断扩展名是否在允许集合中。
     *
     * @param extension 扩展名
     * @param allowExtensions 允许集合
     * @return true 表示允许
     */
    public static boolean isAllowedExtension(String extension, Collection<String> allowExtensions) {
        if (allowExtensions == null || allowExtensions.isEmpty()) {
            return true;
        }
        String ext = normalizeExtension(extension);
        for (String allowExtension : allowExtensions) {
            if (ext.equals(normalizeExtension(allowExtension))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断扩展名是否在禁止集合中。
     *
     * @param extension 扩展名
     * @param denyExtensions 禁止集合
     * @return true 表示禁止
     */
    public static boolean isDeniedExtension(String extension, Collection<String> denyExtensions) {
        if (denyExtensions == null || denyExtensions.isEmpty()) {
            return false;
        }
        String ext = normalizeExtension(extension);
        for (String denyExtension : denyExtensions) {
            if (ext.equals(normalizeExtension(denyExtension))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断文件分类是否在允许集合中。
     *
     * @param category 文件分类
     * @param allowCategories 允许集合
     * @return true 表示允许
     */
    public static boolean isAllowedCategory(FileTypeCategory category, Collection<FileTypeCategory> allowCategories) {
        return allowCategories == null || allowCategories.isEmpty() || allowCategories.contains(category);
    }

    /**
     * 校验本地文件类型。
     *
     * @param path 文件路径
     * @param policy 校验策略
     * @return 校验结果
     */
    public static FileTypeCheckResult validateFileType(Path path, FileTypePolicy policy) {
        FileTypePolicy actualPolicy = policy == null ? FileTypePolicy.defaults() : policy;
        FileTypeInfo info = detectInfo(path);
        List<String> messages = validateInfo(info, actualPolicy);
        return messages.isEmpty() ? FileTypeCheckResult.passed(info) : FileTypeCheckResult.failed(info, messages);
    }

    /**
     * 校验上传流文件类型。
     *
     * @param inputStream 输入流
     * @param fileName 文件名
     * @param policy 校验策略
     * @return 校验结果
     */
    public static FileTypeCheckResult validateFileType(InputStream inputStream, String fileName, FileTypePolicy policy) {
        return checkUploadType(inputStream, fileName, policy);
    }

    /**
     * 构建图片上传策略。
     *
     * @return 图片上传策略
     */
    public static FileTypePolicy buildImagePolicy() {
        return FileTypePolicy.builder()
                .allowMimeTypes(Set.of("image/*"))
                .allowExtensions(Set.of("jpg", "jpeg", "png", "gif", "bmp", "webp", "svg", "tif", "tiff", "ico"))
                .allowCategories(EnumSet.of(FileTypeCategory.IMAGE))
                .build();
    }

    /**
     * 构建文档上传策略。
     *
     * @return 文档上传策略
     */
    public static FileTypePolicy buildDocumentPolicy() {
        return FileTypePolicy.builder()
                .allowCategories(EnumSet.of(FileTypeCategory.DOCUMENT, FileTypeCategory.PDF, FileTypeCategory.WORD,
                        FileTypeCategory.EXCEL, FileTypeCategory.POWERPOINT, FileTypeCategory.TEXT, FileTypeCategory.CODE))
                .allowExtensions(Set.of("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "csv", "json", "xml", "md"))
                .build();
    }

    /**
     * 构建音视频上传策略。
     *
     * @return 音视频上传策略
     */
    public static FileTypePolicy buildMediaPolicy() {
        return FileTypePolicy.builder()
                .allowMimeTypes(Set.of("audio/*", "video/*"))
                .allowCategories(EnumSet.of(FileTypeCategory.AUDIO, FileTypeCategory.VIDEO))
                .allowExtensions(Set.of("mp3", "wav", "flac", "aac", "ogg", "mp4", "mkv", "mov", "avi", "webm", "m4v"))
                .build();
    }

    /**
     * 构建压缩包上传策略。
     *
     * @return 压缩包上传策略
     */
    public static FileTypePolicy buildArchivePolicy() {
        return FileTypePolicy.builder()
                .allowCategories(EnumSet.of(FileTypeCategory.ARCHIVE))
                .allowExtensions(Set.of("zip", "rar", "7z", "tar", "gz", "tgz", "bz2", "xz"))
                .build();
    }

    /**
     * 判断是否危险文件类型。
     *
     * @param mimeType MIME 类型
     * @param extension 扩展名
     * @return true 表示危险类型
     */
    public static boolean isDangerousFile(String mimeType, String extension) {
        return isDangerousMimeType(mimeType) || isDangerousExtension(extension) || isExecutableFile(mimeType, extension) || isScriptFile(mimeType, extension);
    }

    /**
     * 判断是否危险扩展名。
     *
     * @param extension 扩展名
     * @return true 表示危险扩展名
     */
    public static boolean isDangerousExtension(String extension) {
        return DANGEROUS_EXTENSIONS.contains(normalizeExtension(extension));
    }

    /**
     * 判断是否危险 MIME 类型。
     *
     * @param mimeType MIME 类型
     * @return true 表示危险 MIME 类型
     */
    public static boolean isDangerousMimeType(String mimeType) {
        String normalized = normalizeMimeType(mimeType);
        return DANGEROUS_MIME_TYPES.contains(normalized);
    }

    /**
     * 判断是否可执行文件。
     *
     * @param mimeType MIME 类型
     * @param extension 扩展名
     * @return true 表示可执行文件
     */
    public static boolean isExecutableFile(String mimeType, String extension) {
        String ext = normalizeExtension(extension);
        String normalized = normalizeMimeType(mimeType);
        return EXECUTABLE_EXTENSIONS.contains(ext)
                || Set.of("application/x-msdownload", "application/x-msdos-program", "application/x-dosexec", "application/x-executable",
                "application/vnd.microsoft.portable-executable", "application/java-archive", "application/x-sharedlib").contains(normalized);
    }

    /**
     * 判断是否脚本文件。
     *
     * @param mimeType MIME 类型
     * @param extension 扩展名
     * @return true 表示脚本文件
     */
    public static boolean isScriptFile(String mimeType, String extension) {
        String ext = normalizeExtension(extension);
        String normalized = normalizeMimeType(mimeType);
        return SCRIPT_EXTENSIONS.contains(ext)
                || Set.of("application/x-sh", "application/x-shellscript", "application/x-bat", "application/javascript", "text/javascript",
                "text/x-python", "text/x-php", "text/x-perl").contains(normalized);
    }

    /**
     * 判断文件是否疑似伪装。
     *
     * @param fileName 文件名
     * @param detectedMimeType 真实识别出的 MIME 类型
     * @return true 表示疑似伪装
     */
    public static boolean isDisguisedFile(String fileName, String detectedMimeType) {
        return isDoubleExtensionDangerous(fileName) || isMimeSpoofing(fileName, detectedMimeType);
    }

    /**
     * 判断文件扩展名与真实 MIME 类型是否疑似伪造。
     *
     * @param fileName 文件名
     * @param detectedMimeType 真实识别出的 MIME 类型
     * @return true 表示疑似伪造
     */
    public static boolean isMimeSpoofing(String fileName, String detectedMimeType) {
        String extension = getExtension(fileName);
        if (extension.isEmpty()) {
            return false;
        }
        if (isDangerousExtension(extension)) {
            return true;
        }
        String normalizedMime = normalizeMimeType(detectedMimeType);
        if (isUnknownMimeType(normalizedMime)) {
            return false;
        }
        return !isExtensionMatchedMimeTypeByExtension(extension, normalizedMime);
    }

    /**
     * 判断双扩展名是否存在风险。
     *
     * @param fileName 文件名
     * @return true 表示存在风险
     */
    public static boolean isDoubleExtensionDangerous(String fileName) {
        List<String> extensions = getAllExtensions(fileName);
        if (extensions.size() < 2) {
            return false;
        }
        for (String extension : extensions) {
            if (isDangerousExtension(extension)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断文件名中是否包含危险扩展名。
     *
     * @param fileName 文件名
     * @return true 表示包含危险扩展名
     */
    public static boolean containsDangerousExtension(String fileName) {
        for (String extension : getAllExtensions(fileName)) {
            if (isDangerousExtension(extension)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 断言文件类型安全，不安全时抛出异常。
     *
     * @param fileName 文件名
     * @param detectedMimeType 真实识别出的 MIME 类型
     */
    public static void assertSafeFileType(String fileName, String detectedMimeType) {
        FileTypeCheckResult result = checkSafeFileType(fileName, detectedMimeType);
        if (!result.isPassed()) {
            throw new DangerousFileTypeException(String.join("; ", result.getMessages()));
        }
    }

    /**
     * 检查文件类型安全性。
     *
     * @param fileName 文件名
     * @param detectedMimeType 真实识别出的 MIME 类型
     * @return 检查结果
     */
    public static FileTypeCheckResult checkSafeFileType(String fileName, String detectedMimeType) {
        FileTypeInfo info = toFileTypeInfo(fileName, detectedMimeType);
        List<String> messages = new ArrayList<>();
        if (info.isDangerous()) {
            messages.add("文件类型存在安全风险");
        }
        if (isDisguisedFile(fileName, detectedMimeType)) {
            messages.add("文件扩展名与真实类型不一致或存在伪装风险");
        }
        return messages.isEmpty() ? FileTypeCheckResult.passed(info) : FileTypeCheckResult.failed(info, messages);
    }

    /**
     * 根据文件路径识别完整文件类型信息。
     *
     * @param path 文件路径
     * @return 文件类型信息
     */
    public static FileTypeInfo detectInfo(Path path) {
        requireExistingFile(path);
        String fileName = path.getFileName() == null ? "" : path.getFileName().toString();
        return toFileTypeInfo(fileName, detectMimeType(path));
    }

    /**
     * 根据 File 对象识别完整文件类型信息。
     *
     * @param file 文件对象
     * @return 文件类型信息
     */
    public static FileTypeInfo detectInfo(File file) {
        if (file == null) {
            throw new IllegalArgumentException("文件对象不能为空");
        }
        return detectInfo(file.toPath());
    }

    /**
     * 根据输入流和文件名识别完整文件类型信息。
     *
     * @param inputStream 输入流
     * @param fileName 文件名
     * @return 文件类型信息
     */
    public static FileTypeInfo detectInfo(InputStream inputStream, String fileName) {
        return toFileTypeInfo(fileName, detectMimeType(inputStream, fileName));
    }

    /**
     * 根据文件名和 MIME 类型构建文件类型信息。
     *
     * @param fileName 文件名
     * @param mimeType MIME 类型
     * @return 文件类型信息
     */
    public static FileTypeInfo toFileTypeInfo(String fileName, String mimeType) {
        String safeFileName = nullToEmpty(fileName);
        String extension = getExtension(safeFileName);
        String normalizedMime = normalizeMimeType(mimeType);
        FileTypeCategory category = getCategory(normalizedMime, extension);
        boolean known = isKnownMimeType(normalizedMime);
        boolean binary = isBinaryType(normalizedMime);
        boolean dangerous = isDangerousFile(normalizedMime, extension) || containsDangerousExtension(safeFileName);
        boolean extensionMatched = extension.isEmpty() || isExtensionMatchedMimeTypeByExtension(extension, normalizedMime) || isUnknownMimeType(normalizedMime);
        boolean safe = !dangerous && !isDisguisedFile(safeFileName, normalizedMime);
        String mediaType = normalizedMime.isEmpty() ? "" : parseMediaType(normalizedMime).toString();
        String description = getDescription(normalizedMime, extension);
        return new FileTypeInfo(safeFileName, extension, normalizedMime, mediaType, category, known, binary, safe, extensionMatched, dangerous, description);
    }

    /**
     * 获取文件类型描述。
     *
     * @param mimeType MIME 类型
     * @param extension 扩展名
     * @return 文件类型描述
     */
    public static String getDescription(String mimeType, String extension) {
        String normalizedMime = normalizeMimeType(mimeType);
        FileTypeCategory category = getCategory(normalizedMime, extension);
        if (isUnknownMimeType(normalizedMime)) {
            return category.getDisplayName();
        }
        return category.getDisplayName() + "（" + normalizedMime + "）";
    }

    /**
     * 获取文件分类展示名称。
     *
     * @param category 文件分类
     * @return 展示名称
     */
    public static String getDisplayName(FileTypeCategory category) {
        return category == null ? FileTypeCategory.UNKNOWN.getDisplayName() : category.getDisplayName();
    }

    /**
     * 获取适合 HTTP 响应的 Content-Type。
     *
     * @param path 文件路径
     * @return Content-Type
     */
    public static String getContentType(Path path) {
        String mimeType = detectMimeType(path);
        return mimeType.isEmpty() ? DEFAULT_MIME_TYPE : mimeType;
    }

    /**
     * 根据文件名和兜底 MIME 类型获取 Content-Type。
     *
     * @param fileName 文件名
     * @param fallbackMimeType 兜底 MIME 类型
     * @return Content-Type
     */
    public static String getContentType(String fileName, String fallbackMimeType) {
        String fromExtension = getMimeTypeByExtension(getExtension(fileName));
        if (!isUnknownMimeType(fromExtension)) {
            return fromExtension;
        }
        String fallback = normalizeMimeType(fallbackMimeType);
        return fallback.isEmpty() ? DEFAULT_MIME_TYPE : fallback;
    }

    /**
     * 获取默认 Content-Type。
     *
     * @return 默认 Content-Type
     */
    public static String getDefaultContentType() {
        return DEFAULT_MIME_TYPE;
    }

    /**
     * 获取适合浏览器预览的 Content-Type。
     *
     * @param mimeType MIME 类型
     * @return 适合预览的 Content-Type，不适合预览时返回 application/octet-stream
     */
    public static String getPreviewContentType(String mimeType) {
        String normalized = normalizeMimeType(mimeType);
        return isInlineDisplayable(normalized) ? normalized : DEFAULT_MIME_TYPE;
    }

    /**
     * 判断是否支持浏览器预览。
     *
     * @param mimeType MIME 类型
     * @param extension 扩展名
     * @return true 表示支持预览
     */
    public static boolean isPreviewable(String mimeType, String extension) {
        return isInlineDisplayable(mimeType)
                || isJson(mimeType, extension)
                || isXml(mimeType, extension)
                || isCsv(mimeType, extension)
                || isMarkdown(mimeType, extension);
    }

    /**
     * 判断是否适合 inline 展示。
     *
     * @param mimeType MIME 类型
     * @return true 表示适合 inline 展示
     */
    public static boolean isInlineDisplayable(String mimeType) {
        String normalized = normalizeMimeType(mimeType);
        return isImage(normalized)
                || isAudio(normalized)
                || isVideo(normalized)
                || isPdf(normalized)
                || isTextType(normalized)
                || "application/json".equals(normalized)
                || "application/xml".equals(normalized);
    }

    /**
     * 判断是否建议强制下载。
     *
     * @param mimeType MIME 类型
     * @param extension 扩展名
     * @return true 表示建议强制下载
     */
    public static boolean isDownloadOnly(String mimeType, String extension) {
        return isDangerousFile(mimeType, extension) || !isPreviewable(mimeType, extension);
    }

    /**
     * 根据 MIME 类型获取下载时推荐扩展名。
     *
     * @param mimeType MIME 类型
     * @return 推荐扩展名，不存在时返回空字符串
     */
    public static String getRecommendedExtension(String mimeType) {
        return getExtensionByMimeType(mimeType);
    }

    /**
     * 获取安全下载文件名。
     *
     * @param fileName 原始文件名
     * @param mimeType MIME 类型
     * @return 安全下载文件名
     */
    public static String getSafeDownloadFileName(String fileName, String mimeType) {
        String rawName = nullToEmpty(fileName);
        int separatorIndex = Math.max(rawName.lastIndexOf('/'), rawName.lastIndexOf('\\'));
        if (separatorIndex >= 0) {
            rawName = rawName.substring(separatorIndex + 1);
        }
        String normalizedName = Normalizer.normalize(rawName, Normalizer.Form.NFKC)
                .replaceAll("[\\r\\n\\t\\x00]", "")
                .replace('/', '_')
                .replace('\\', '_')
                .trim();
        if (normalizedName.isEmpty() || ".".equals(normalizedName) || "..".equals(normalizedName)) {
            normalizedName = "download";
        }
        String recommendedExtension = getRecommendedExtension(mimeType);
        String currentExtension = getExtension(normalizedName);
        if (!recommendedExtension.isEmpty() && currentExtension.isEmpty()) {
            normalizedName = normalizedName + "." + recommendedExtension;
        }
        return normalizedName;
    }

    /**
     * 批量识别文件 MIME 类型。
     *
     * @param paths 文件路径集合
     * @return 路径与 MIME 类型映射
     */
    public static Map<Path, String> detectBatch(Collection<Path> paths) {
        if (paths == null || paths.isEmpty()) {
            return Map.of();
        }
        Map<Path, String> result = new LinkedHashMap<>();
        for (Path path : paths) {
            result.put(path, detectMimeType(path));
        }
        return Collections.unmodifiableMap(result);
    }

    /**
     * 批量识别完整文件类型信息。
     *
     * @param paths 文件路径集合
     * @return 文件类型信息列表
     */
    public static List<FileTypeInfo> detectInfoBatch(Collection<Path> paths) {
        if (paths == null || paths.isEmpty()) {
            return List.of();
        }
        List<FileTypeInfo> result = new ArrayList<>();
        for (Path path : paths) {
            result.add(detectInfo(path));
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * 按文件分类分组。
     *
     * @param fileTypes 文件类型信息集合
     * @return 分类分组结果
     */
    public static Map<FileTypeCategory, List<FileTypeInfo>> groupByCategory(Collection<FileTypeInfo> fileTypes) {
        if (fileTypes == null || fileTypes.isEmpty()) {
            return Map.of();
        }
        Map<FileTypeCategory, List<FileTypeInfo>> result = new EnumMap<>(FileTypeCategory.class);
        for (FileTypeInfo info : fileTypes) {
            if (info != null) {
                result.computeIfAbsent(info.getCategory(), key -> new ArrayList<>()).add(info);
            }
        }
        return immutableListMap(result);
    }

    /**
     * 按 MIME 类型分组。
     *
     * @param fileTypes 文件类型信息集合
     * @return MIME 类型分组结果
     */
    public static Map<String, List<FileTypeInfo>> groupByMimeType(Collection<FileTypeInfo> fileTypes) {
        if (fileTypes == null || fileTypes.isEmpty()) {
            return Map.of();
        }
        Map<String, List<FileTypeInfo>> result = new LinkedHashMap<>();
        for (FileTypeInfo info : fileTypes) {
            if (info != null) {
                result.computeIfAbsent(info.getMimeType(), key -> new ArrayList<>()).add(info);
            }
        }
        return immutableListMap(result);
    }

    /**
     * 按文件分类统计数量。
     *
     * @param fileTypes 文件类型信息集合
     * @return 分类数量统计
     */
    public static Map<FileTypeCategory, Long> countByCategory(Collection<FileTypeInfo> fileTypes) {
        if (fileTypes == null || fileTypes.isEmpty()) {
            return Map.of();
        }
        Map<FileTypeCategory, Long> result = new EnumMap<>(FileTypeCategory.class);
        for (FileTypeInfo info : fileTypes) {
            if (info != null) {
                result.merge(info.getCategory(), 1L, Long::sum);
            }
        }
        return Collections.unmodifiableMap(result);
    }

    /**
     * 按文件分类过滤。
     *
     * @param fileTypes 文件类型信息集合
     * @param category 文件分类
     * @return 过滤后的文件类型信息列表
     */
    public static List<FileTypeInfo> filterByCategory(Collection<FileTypeInfo> fileTypes, FileTypeCategory category) {
        if (fileTypes == null || fileTypes.isEmpty() || category == null) {
            return List.of();
        }
        return fileTypes.stream()
                .filter(Objects::nonNull)
                .filter(info -> info.getCategory() == category)
                .toList();
    }

    /**
     * 按 MIME 类型过滤。
     *
     * @param fileTypes 文件类型信息集合
     * @param mimeType MIME 类型
     * @return 过滤后的文件类型信息列表
     */
    public static List<FileTypeInfo> filterByMimeType(Collection<FileTypeInfo> fileTypes, String mimeType) {
        String normalized = normalizeMimeType(mimeType);
        if (fileTypes == null || fileTypes.isEmpty() || normalized.isEmpty()) {
            return List.of();
        }
        return fileTypes.stream()
                .filter(Objects::nonNull)
                .filter(info -> normalized.equals(info.getMimeType()))
                .toList();
    }

    /**
     * 过滤危险文件。
     *
     * @param fileTypes 文件类型信息集合
     * @return 危险文件列表
     */
    public static List<FileTypeInfo> filterDangerousFiles(Collection<FileTypeInfo> fileTypes) {
        if (fileTypes == null || fileTypes.isEmpty()) {
            return List.of();
        }
        return fileTypes.stream().filter(Objects::nonNull).filter(FileTypeInfo::isDangerous).toList();
    }

    /**
     * 过滤未知类型文件。
     *
     * @param fileTypes 文件类型信息集合
     * @return 未知类型文件列表
     */
    public static List<FileTypeInfo> filterUnknownFiles(Collection<FileTypeInfo> fileTypes) {
        if (fileTypes == null || fileTypes.isEmpty()) {
            return List.of();
        }
        return fileTypes.stream().filter(Objects::nonNull).filter(info -> !info.isKnown()).toList();
    }

    /**
     * 加载自定义 Tika 配置。
     *
     * @param configPath Tika 配置文件路径
     * @return Tika 配置
     */
    public static TikaConfig loadTikaConfig(Path configPath) {
        if (configPath == null) {
            throw new IllegalArgumentException("Tika 配置文件路径不能为空");
        }
        if (!Files.exists(configPath) || !Files.isRegularFile(configPath)) {
            throw new IllegalArgumentException("Tika 配置文件不存在或不是普通文件: " + configPath);
        }
        try {
            return new TikaConfig(configPath);
        } catch (TikaException | SAXException | IOException | RuntimeException e) {
            throw new FileTypeException("加载 Tika 配置失败: " + configPath, e);
        }
    }

    /**
     * 根据配置文件创建 Tika 实例。
     *
     * @param configPath Tika 配置文件路径
     * @return Tika 实例
     */
    public static Tika createTika(Path configPath) {
        return new Tika(loadTikaConfig(configPath));
    }

    /**
     * 使用指定 Tika 配置识别文件 MIME 类型。
     *
     * @param path 文件路径
     * @param tikaConfig Tika 配置
     * @return MIME 类型
     */
    public static String detectMimeType(Path path, TikaConfig tikaConfig) {
        requireExistingFile(path);
        if (tikaConfig == null) {
            throw new IllegalArgumentException("Tika 配置不能为空");
        }
        try {
            return normalizeMimeType(new Tika(tikaConfig).detect(path));
        } catch (IOException e) {
            throw new FileTypeException("使用指定 Tika 配置识别 MIME 类型失败: " + path, e);
        }
    }

    /**
     * 使用指定 Tika 配置识别上传流 MIME 类型。
     *
     * @param inputStream 输入流
     * @param fileName 文件名
     * @param tikaConfig Tika 配置
     * @return MIME 类型
     */
    public static String detectMimeType(InputStream inputStream, String fileName, TikaConfig tikaConfig) {
        InputStream stream = requireInputStream(inputStream);
        if (tikaConfig == null) {
            throw new IllegalArgumentException("Tika 配置不能为空");
        }
        try {
            return normalizeMimeType(new Tika(tikaConfig).detect(ensureBuffered(stream), nullToEmpty(fileName)));
        } catch (IOException e) {
            throw new FileTypeException("使用指定 Tika 配置识别 MIME 类型失败: " + nullToEmpty(fileName), e);
        }
    }

    /**
     * 注册自定义扩展名与 MIME 类型映射。
     *
     * @param extension 扩展名
     * @param mimeType MIME 类型
     */
    public static void registerCustomMimeMapping(String extension, String mimeType) {
        String ext = normalizeExtension(extension);
        String normalizedMime = normalizeMimeType(mimeType);
        if (ext.isEmpty()) {
            throw new IllegalArgumentException("扩展名不能为空");
        }
        if (!isValidMimeFormat(normalizedMime)) {
            throw new IllegalArgumentException("MIME 类型格式不正确: " + mimeType);
        }
        CUSTOM_EXTENSION_TO_MIME.put(ext, normalizedMime);
        CUSTOM_MIME_TO_EXTENSION.put(normalizedMime, ext);
    }

    /**
     * 根据扩展名获取自定义 MIME 类型。
     *
     * @param extension 扩展名
     * @return 自定义 MIME 类型，不存在时返回空字符串
     */
    public static String getCustomMimeTypeByExtension(String extension) {
        return CUSTOM_EXTENSION_TO_MIME.getOrDefault(normalizeExtension(extension), "");
    }

    /**
     * 根据 MIME 类型获取自定义扩展名。
     *
     * @param mimeType MIME 类型
     * @return 自定义扩展名，不存在时返回空字符串
     */
    public static String getCustomExtensionByMimeType(String mimeType) {
        return CUSTOM_MIME_TO_EXTENSION.getOrDefault(normalizeMimeType(mimeType), "");
    }

    /**
     * 清空自定义 MIME 映射。
     */
    public static void clearCustomMimeMappings() {
        CUSTOM_EXTENSION_TO_MIME.clear();
        CUSTOM_MIME_TO_EXTENSION.clear();
    }

    /**
     * 获取当前支持的自定义 MIME 类型集合。
     *
     * @return 自定义 MIME 类型集合
     */
    public static Set<String> getSupportedCustomMimeTypes() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(CUSTOM_MIME_TO_EXTENSION.keySet()));
    }

    static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static InputStream requireInputStream(InputStream inputStream) {
        if (inputStream == null) {
            throw new IllegalArgumentException("输入流不能为空");
        }
        return inputStream;
    }

    private static void requireExistingFile(Path path) {
        if (path == null) {
            throw new IllegalArgumentException("文件路径不能为空");
        }
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("文件不存在: " + path);
        }
        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException("路径不是普通文件: " + path);
        }
    }

    private static InputStream ensureBuffered(InputStream inputStream) {
        return inputStream.markSupported() ? inputStream : new BufferedInputStream(inputStream);
    }

    private static MediaType parseMediaType(String mimeType) {
        String normalized = normalizeMimeType(mimeType);
        if (normalized.isEmpty()) {
            return MediaType.OCTET_STREAM;
        }
        MediaType mediaType = MediaType.parse(normalized);
        return mediaType == null ? MediaType.OCTET_STREAM : mediaType;
    }

    private static String extractExtensionOrSelf(String fileNameOrExtension) {
        String value = nullToEmpty(fileNameOrExtension).trim();
        if (value.isEmpty()) {
            return "";
        }
        String fromName = getExtension(value);
        return fromName.isEmpty() ? normalizeExtension(value) : fromName;
    }

    private static List<String> validateInfo(FileTypeInfo info, FileTypePolicy policy) {
        List<String> messages = new ArrayList<>();
        if (policy.isRejectUnknownType() && !info.isKnown()) {
            messages.add("未知文件类型不允许上传");
        }
        if (policy.isRejectDangerousType() && info.isDangerous()) {
            messages.add("危险文件类型不允许上传");
        }
        if (!policy.getAllowMimeTypes().isEmpty() && !matchesMimeCollection(info.getMimeType(), policy.getAllowMimeTypes(), policy.isAllowWildcardMimeType())) {
            messages.add("MIME 类型不在允许范围内: " + info.getMimeType());
        }
        if (!policy.getDenyMimeTypes().isEmpty() && matchesMimeCollection(info.getMimeType(), policy.getDenyMimeTypes(), policy.isAllowWildcardMimeType())) {
            messages.add("MIME 类型在禁止范围内: " + info.getMimeType());
        }
        if (!policy.getAllowExtensions().isEmpty() && !policy.getAllowExtensions().contains(info.getExtension())) {
            messages.add("扩展名不在允许范围内: " + info.getExtension());
        }
        if (!policy.getDenyExtensions().isEmpty() && policy.getDenyExtensions().contains(info.getExtension())) {
            messages.add("扩展名在禁止范围内: " + info.getExtension());
        }
        if (!policy.getAllowCategories().isEmpty() && !policy.getAllowCategories().contains(info.getCategory())) {
            messages.add("文件分类不在允许范围内: " + info.getCategory());
        }
        if (!policy.getDenyCategories().isEmpty() && policy.getDenyCategories().contains(info.getCategory())) {
            messages.add("文件分类在禁止范围内: " + info.getCategory());
        }
        if (policy.isCheckExtensionMatch() && !info.getExtension().isEmpty() && !info.isExtensionMatched()) {
            messages.add("文件扩展名与真实类型不匹配");
        }
        return messages;
    }

    private static boolean matchesMimeCollection(String mimeType, Collection<String> patterns, boolean allowWildcard) {
        if (patterns == null || patterns.isEmpty()) {
            return false;
        }
        for (String pattern : patterns) {
            if (allowWildcard) {
                if (isMimeTypeMatched(mimeType, pattern)) {
                    return true;
                }
            } else if (isMimeType(mimeType, pattern)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isValidMimeFormat(String mimeType) {
        String normalized = normalizeMimeType(mimeType);
        int index = normalized.indexOf('/');
        return index > 0 && index < normalized.length() - 1 && !normalized.contains(" ");
    }

    private static <K> Map<K, List<FileTypeInfo>> immutableListMap(Map<K, List<FileTypeInfo>> source) {
        Map<K, List<FileTypeInfo>> result = new LinkedHashMap<>();
        for (Map.Entry<K, List<FileTypeInfo>> entry : source.entrySet()) {
            result.put(entry.getKey(), Collections.unmodifiableList(entry.getValue()));
        }
        return Collections.unmodifiableMap(result);
    }

    private static Map<String, String> buildExtensionToMimeMap() {
        Map<String, String> map = new LinkedHashMap<>();
        put(map, "txt", "text/plain");
        put(map, "text", "text/plain");
        put(map, "log", "text/plain");
        put(map, "csv", "text/csv");
        put(map, "json", "application/json");
        put(map, "xml", "application/xml");
        put(map, "yaml", "application/x-yaml");
        put(map, "yml", "application/x-yaml");
        put(map, "md", "text/markdown");
        put(map, "markdown", "text/markdown");
        put(map, "html", "text/html");
        put(map, "htm", "text/html");
        put(map, "css", "text/css");
        put(map, "js", "text/javascript");
        put(map, "mjs", "text/javascript");
        put(map, "ts", "text/x-typescript");
        put(map, "java", "text/x-java-source");
        put(map, "kt", "text/x-kotlin");
        put(map, "sql", "application/sql");
        put(map, "pdf", "application/pdf");
        put(map, "doc", "application/msword");
        put(map, "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        put(map, "xls", "application/vnd.ms-excel");
        put(map, "xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        put(map, "ppt", "application/vnd.ms-powerpoint");
        put(map, "pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation");
        put(map, "odt", "application/vnd.oasis.opendocument.text");
        put(map, "ods", "application/vnd.oasis.opendocument.spreadsheet");
        put(map, "odp", "application/vnd.oasis.opendocument.presentation");
        put(map, "jpg", "image/jpeg");
        put(map, "jpeg", "image/jpeg");
        put(map, "png", "image/png");
        put(map, "gif", "image/gif");
        put(map, "bmp", "image/bmp");
        put(map, "webp", "image/webp");
        put(map, "svg", "image/svg+xml");
        put(map, "ico", "image/x-icon");
        put(map, "tif", "image/tiff");
        put(map, "tiff", "image/tiff");
        put(map, "mp3", "audio/mpeg");
        put(map, "wav", "audio/wav");
        put(map, "flac", "audio/flac");
        put(map, "aac", "audio/aac");
        put(map, "ogg", "audio/ogg");
        put(map, "mp4", "video/mp4");
        put(map, "m4v", "video/x-m4v");
        put(map, "mkv", "video/x-matroska");
        put(map, "mov", "video/quicktime");
        put(map, "avi", "video/x-msvideo");
        put(map, "webm", "video/webm");
        put(map, "zip", "application/zip");
        put(map, "rar", "application/vnd.rar");
        put(map, "7z", "application/x-7z-compressed");
        put(map, "tar", "application/x-tar");
        put(map, "gz", "application/gzip");
        put(map, "tgz", "application/gzip");
        put(map, "bz2", "application/x-bzip2");
        put(map, "xz", "application/x-xz");
        put(map, "ttf", "font/ttf");
        put(map, "otf", "font/otf");
        put(map, "woff", "font/woff");
        put(map, "woff2", "font/woff2");
        put(map, "eot", "application/vnd.ms-fontobject");
        put(map, "eml", "message/rfc822");
        put(map, "msg", "application/vnd.ms-outlook");
        put(map, "db", "application/octet-stream");
        put(map, "sqlite", "application/vnd.sqlite3");
        put(map, "sqlite3", "application/vnd.sqlite3");
        put(map, "exe", "application/x-msdownload");
        put(map, "msi", "application/x-ms-installer");
        put(map, "dll", "application/x-msdownload");
        put(map, "sh", "application/x-sh");
        put(map, "bat", "application/x-bat");
        put(map, "cmd", "application/x-bat");
        put(map, "ps1", "application/x-powershell");
        put(map, "jar", "application/java-archive");
        put(map, "war", "application/java-archive");
        return Collections.unmodifiableMap(map);
    }

    private static void put(Map<String, String> map, String extension, String mimeType) {
        map.put(normalizeExtension(extension), normalizeMimeType(mimeType));
    }

    private static Map<String, Set<String>> buildMimeToExtensionsMap(Map<String, String> extensionToMime) {
        Map<String, Set<String>> result = new HashMap<>();
        for (Map.Entry<String, String> entry : extensionToMime.entrySet()) {
            result.computeIfAbsent(entry.getValue(), key -> new LinkedHashSet<>()).add(entry.getKey());
        }
        Map<String, Set<String>> sorted = new LinkedHashMap<>();
        List<Map.Entry<String, Set<String>>> entries = new ArrayList<>(result.entrySet());
        entries.sort(Map.Entry.comparingByKey());
        Comparator<String> comparator = extensionPriorityComparator();
        for (Map.Entry<String, Set<String>> entry : entries) {
            List<String> extensions = new ArrayList<>(entry.getValue());
            extensions.sort(comparator);
            sorted.put(entry.getKey(), Collections.unmodifiableSet(new LinkedHashSet<>(extensions)));
        }
        return Collections.unmodifiableMap(sorted);
    }

    private static Comparator<String> extensionPriorityComparator() {
        Set<String> preferred = new HashSet<>(Set.of("jpg", "txt", "htm", "doc", "xls", "ppt", "gz"));
        return (left, right) -> {
            boolean leftPreferred = preferred.contains(left);
            boolean rightPreferred = preferred.contains(right);
            if (leftPreferred && !rightPreferred) {
                return -1;
            }
            if (!leftPreferred && rightPreferred) {
                return 1;
            }
            return left.compareTo(right);
        };
    }
}
