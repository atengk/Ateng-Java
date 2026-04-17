package local.ateng.java.customutils.utils;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 文件工具类
 *
 * @author Ateng
 * @since 2025-07-19
 */
public final class FileUtil {

    /**
     * 递归删除目录时的最大文件数量阈值，超过该值则拒绝删除，防止误删
     */
    private static final int MAX_DELETE_FILE_COUNT = 100;
    /**
     * 缓冲区大小
     */
    private static final int BUFFER_SIZE = 8192;
    /**
     * 扩展名前的点
     */
    private static final String EXTENSION_DOT = ".";

    /**
     * 禁止实例化工具类
     */
    private FileUtil() {
        throw new UnsupportedOperationException("工具类不可实例化");
    }

    /**
     * 判断路径是否存在
     *
     * @param path 路径
     * @return true 表示存在
     */
    public static boolean exists(Path path) {
        return path != null && Files.exists(path);
    }

    /**
     * 判断路径是否为文件
     *
     * @param path 路径
     * @return true 表示是普通文件
     */
    public static boolean isFile(Path path) {
        return exists(path) && Files.isRegularFile(path);
    }

    /**
     * 判断路径是否为目录
     *
     * @param path 路径
     * @return true 表示是目录
     */
    public static boolean isDirectory(Path path) {
        return exists(path) && Files.isDirectory(path);
    }

    /**
     * 获取相对路径（B 相对于 A 的路径）
     *
     * @param baseDir 基准目录（A）
     * @param target  目标路径（B）
     * @return 相对路径（baseDir.relativize(target)）
     * @throws IllegalArgumentException 参数不能为空或不规范
     */
    public static Path getRelativePath(Path baseDir, Path target) {
        if (baseDir == null || target == null) {
            throw new IllegalArgumentException("基准路径或目标路径不能为空");
        }
        return baseDir.toAbsolutePath().normalize().relativize(
                target.toAbsolutePath().normalize());
    }

    /**
     * 获取安全相对路径（不同根返回 null）
     *
     * @param baseDir 基准目录（A）
     * @param target  目标路径（B）
     * @return 相对路径（baseDir.relativize(target)）
     */
    public static Path getRelativizeSafely(Path baseDir, Path target) {
        if (baseDir == null || target == null) {
            return null;
        }
        Path absBase = baseDir.toAbsolutePath().normalize();
        Path absTarget = target.toAbsolutePath().normalize();

        try {
            return absBase.relativize(absTarget);
        } catch (IllegalArgumentException e) {
            // 不在同一个根路径下无法 relativize
            return null;
        }
    }

    /**
     * 获取文件名（不包含路径）
     *
     * @param path 文件或目录路径
     * @return 文件名
     */
    public static String getFileName(Path path) {
        if (path == null) {
            return "";
        }
        Path fileName = path.getFileName();
        return fileName != null ? fileName.toString() : "";
    }

    /**
     * 获取文件类型（即扩展名，如 txt、jpg）
     *
     * @param path 文件路径
     * @return 扩展名（无点），没有则返回空串
     */
    public static String getFileExtension(Path path) {
        if (path == null) {
            return "";
        }
        String fileName = getFileName(path);
        return getFileExtension(fileName);
    }

    /**
     * 获取文件类型（即扩展名，如 txt、jpg）
     *
     * @param path 文件路径
     * @return 扩展名（不带点），没有则返回空串
     */
    public static String getFileExtension(String path) {
        return getFileExtension(path, false);
    }

    /**
     * 获取文件类型（即扩展名，如 txt、jpg 或 .txt、.jpg）
     *
     * @param path    文件路径字符串
     * @param withDot 是否包含“.”前缀
     * @return 扩展名（根据参数控制是否带点），没有则返回空串
     */
    public static String getFileExtension(String path, boolean withDot) {
        if (path == null || path.trim().isEmpty()) {
            return "";
        }

        String fileName = new File(path).getName();
        int index = fileName.lastIndexOf(EXTENSION_DOT);

        // 没有 . 或 . 在开头（隐藏文件）或 . 是最后一个字符，说明无扩展名
        if (index <= 0 || index == fileName.length() - 1) {
            return "";
        }

        return withDot ? fileName.substring(index) : fileName.substring(index + 1);
    }

    /**
     * 根据文件扩展名获取大致文件类型分类（如 image、video、audio、text、application 等）
     *
     * @param fileExtension 文件扩展名（可以带点或不带点，大小写不敏感）
     * @return 文件大类（如 image、video、audio、text、application、archive 等），无法识别则返回 "unknown"
     */
    public static String getMimeCategory(String fileExtension) {
        if (fileExtension == null || fileExtension.trim().isEmpty()) {
            return "unknown";
        }

        // 去除开头的点并转换为小写
        String ext = fileExtension.trim().toLowerCase();
        if (ext.startsWith(EXTENSION_DOT)) {
            ext = ext.substring(1);
        }

        switch (ext) {
            // 图片
            case "jpg":
            case "jpeg":
            case "png":
            case "gif":
            case "bmp":
            case "webp":
            case "tiff":
            case "ico":
            case "svg":
                return "image";

            // 视频
            case "mp4":
            case "avi":
            case "mov":
            case "wmv":
            case "flv":
            case "mkv":
            case "webm":
            case "3gp":
            case "mpeg":
                return "video";

            // 音频
            case "mp3":
            case "wav":
            case "aac":
            case "flac":
            case "ogg":
            case "wma":
            case "m4a":
            case "amr":
                return "audio";

            // 文本
            case "txt":
            case "csv":
            case "log":
            case "md":
            case "json":
            case "xml":
            case "yaml":
            case "yml":
            case "ini":
                return "text";

            // 文档
            case "doc":
            case "docx":
            case "xls":
            case "xlsx":
            case "ppt":
            case "pptx":
            case "pdf":
            case "odt":
            case "ods":
            case "odp":
                return "document";

            // 压缩包
            case "zip":
            case "rar":
            case "7z":
            case "tar":
            case "gz":
            case "bz2":
            case "xz":
            case "iso":
                return "archive";

            // 可执行文件
            case "exe":
            case "msi":
            case "bat":
            case "sh":
            case "apk":
            case "jar":
            case "bin":
            case "cmd":
                return "executable";

            // 编程代码
            case "java":
            case "py":
            case "js":
            case "ts":
            case "html":
            case "css":
            case "cpp":
            case "c":
            case "h":
            case "go":
            case "rs":
            case "php":
            case "rb":
            case "sql":
            case "kt":
                return "code";

            // 应用类型
            case "swf":
            case "xhtml":
            case "xsd":
            case "wsdl":
            case "psd":
            case "ai":
            case "sketch":
                return "application";

            default:
                return "unknown";
        }
    }

    /**
     * 根据文件扩展名获取 MIME 类型（不含点），不区分大小写
     *
     * @param fileExtension 文件扩展名（如 "jpg", "txt"），无需带点
     * @return MIME 类型，未知类型返回 "application/octet-stream"
     */
    public static String getMimeType(String fileExtension) {
        if (fileExtension == null || fileExtension.trim().isEmpty()) {
            return "application/octet-stream";
        }

        String ext = fileExtension.trim().toLowerCase();

        switch (ext) {
            // 文本
            case "txt":
                return "text/plain";
            case "csv":
                return "text/csv";
            case "html":
            case "htm":
                return "text/html";
            case "xml":
                return "application/xml";
            case "json":
                return "application/json";

            // 图片
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "bmp":
                return "image/bmp";
            case "webp":
                return "image/webp";
            case "svg":
                return "image/svg+xml";
            case "ico":
                return "image/x-icon";

            // 文档
            case "pdf":
                return "application/pdf";
            case "doc":
                return "application/msword";
            case "docx":
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls":
                return "application/vnd.ms-excel";
            case "xlsx":
                return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "ppt":
                return "application/vnd.ms-powerpoint";
            case "pptx":
                return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "rtf":
                return "application/rtf";

            // 压缩包
            case "zip":
                return "application/zip";
            case "rar":
                return "application/x-rar-compressed";
            case "7z":
                return "application/x-7z-compressed";
            case "gz":
                return "application/gzip";
            case "tar":
                return "application/x-tar";

            // 音频
            case "mp3":
                return "audio/mpeg";
            case "wav":
                return "audio/wav";
            case "ogg":
                return "audio/ogg";
            case "m4a":
                return "audio/mp4";

            // 视频
            case "mp4":
                return "video/mp4";
            case "avi":
                return "video/x-msvideo";
            case "mov":
                return "video/quicktime";
            case "mkv":
                return "video/x-matroska";
            case "webm":
                return "video/webm";
            case "flv":
                return "video/x-flv";

            // 字体
            case "ttf":
                return "font/ttf";
            case "otf":
                return "font/otf";
            case "woff":
                return "font/woff";
            case "woff2":
                return "font/woff2";

            // 脚本与代码
            case "js":
                return "application/javascript";
            case "css":
                return "text/css";
            case "java":
                return "text/x-java-source";
            case "py":
                return "text/x-python";
            case "json5":
                return "application/json";

            // 默认
            default:
                return "application/octet-stream";
        }
    }

    /**
     * 获取文件大小（单位：字节）
     *
     * @param path 文件路径
     * @return 文件大小（long），不存在返回 -1
     */
    public static long getFileSize(Path path) {
        if (path == null || !Files.exists(path) || !Files.isRegularFile(path)) {
            return -1L;
        }
        try {
            return Files.size(path);
        } catch (IOException e) {
            return -1L;
        }
    }

    /**
     * 判断 child 是否为 parent 的子路径（逻辑包含关系）
     *
     * @param parent 父路径
     * @param child  子路径
     * @return true 表示 child 在 parent 内部
     */
    public static boolean isSubPath(Path parent, Path child) {
        if (parent == null || child == null) {
            return false;
        }

        Path normParent = parent.toAbsolutePath().normalize();
        Path normChild = child.toAbsolutePath().normalize();

        return normChild.startsWith(normParent);
    }

    /**
     * 获取绝对路径并规范化
     *
     * @param path 任意路径
     * @return 规范后的绝对路径
     */
    public static Path toAbsoluteNormalized(Path path) {
        return path == null ? null : path.toAbsolutePath().normalize();
    }

    /**
     * 获取不带扩展名的文件路径
     *
     * @param path 文件路径
     * @return 去掉扩展名的路径字符串
     */
    public static String getPathWithoutExtension(Path path) {
        if (path == null) {
            return "";
        }
        String fileName = getFileName(path);
        int index = fileName.lastIndexOf(".");
        return index > 0 ? fileName.substring(0, index) : fileName;
    }

    /**
     * 安全拼接子路径（防止 null 或非法）
     *
     * @param base     基础目录
     * @param relative 相对路径（文件名、子目录）
     * @return 拼接后的路径
     */
    public static Path getResolveSafe(Path base, String relative) {
        if (base == null || relative == null || relative.trim().isEmpty()) {
            return base;
        }
        return base.resolve(relative.trim()).normalize();
    }

    /**
     * 获取真实路径（包含符号链接解析）
     *
     * @param path 路径
     * @return 真实路径
     * @throws IOException 获取失败
     */
    public static Path getCanonicalPath(Path path) throws IOException {
        return path == null ? null : path.toRealPath();
    }

    /**
     * 创建目录（含父级）
     *
     * @param dirPath 目录路径
     * @throws IOException 创建失败抛出异常
     */
    public static void createDirectories(Path dirPath) throws IOException {
        if (dirPath == null) {
            throw new IllegalArgumentException("目录路径不能为空");
        }
        Files.createDirectories(dirPath);
    }

    /**
     * 创建新文件（如果不存在），自动创建父目录
     *
     * @param filePath 文件路径
     * @throws IOException 创建失败抛出异常
     */
    public static void createFileIfNotExists(Path filePath) throws IOException {
        if (filePath == null) {
            throw new IllegalArgumentException("文件路径不能为空");
        }
        if (!Files.exists(filePath)) {
            Path parent = filePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.createFile(filePath);
        }
    }

    /**
     * 创建系统默认位置的临时目录
     *
     * @return 临时目录路径
     * @throws IOException 创建失败抛出异常
     */
    public static Path createTempDirectory() throws IOException {
        return Files.createTempDirectory("temp_");
    }

    /**
     * 创建系统默认位置的临时目录，指定前缀
     *
     * @param prefix 临时目录前缀
     * @return 临时目录路径
     * @throws IOException 创建失败抛出异常
     */
    public static Path createTempDirectory(String prefix) throws IOException {
        if (prefix == null) {
            prefix = "temp_";
        }
        return Files.createTempDirectory(prefix);
    }

    /**
     * 在指定目录中创建临时目录
     *
     * @param parentDir 临时目录的父目录
     * @param prefix    目录名前缀
     * @return 临时目录路径
     * @throws IOException 创建失败抛出异常
     */
    public static Path createTempDirectory(Path parentDir, String prefix) throws IOException {
        if (parentDir == null) {
            throw new IllegalArgumentException("父级目录不能为空");
        }
        if (!Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
        }
        if (prefix == null) {
            prefix = "temp_";
        }
        return Files.createTempDirectory(parentDir, prefix);
    }

    /**
     * 在指定临时目录中创建文件
     *
     * @param tempDir  临时目录
     * @param fileName 文件名
     * @return 文件路径
     * @throws IOException 创建失败抛出异常
     */
    public static Path createTempFileInDirectory(Path tempDir, String fileName) throws IOException {
        if (!Files.exists(tempDir) || !Files.isDirectory(tempDir)) {
            throw new IOException("临时目录不存在或不是目录：" + tempDir);
        }
        Path filePath = tempDir.resolve(fileName);
        if (!Files.exists(filePath)) {
            Files.createFile(filePath);
        }
        return filePath;
    }

    /**
     * 删除临时目录及其内容（带文件数限制）
     *
     * @param tempDir            临时目录路径
     * @param maxDeleteFileCount 限制目录下最大文件数，超过该数不执行删除
     * @throws IOException 删除失败抛出异常
     */
    public static void deleteTempDirectory(Path tempDir, Integer maxDeleteFileCount) throws IOException {
        if (Files.notExists(tempDir)) {
            return;
        }
        if (!isInSystemTempDir(tempDir)) {
            throw new SecurityException("禁止删除非系统临时目录：" + tempDir);
        }
        if (!Files.isDirectory(tempDir)) {
            throw new IOException("不是有效的临时目录：" + tempDir);
        }
        // 复用安全递归删除
        deleteRecursively(tempDir, maxDeleteFileCount);
    }

    /**
     * 删除临时目录及其内容（带文件数限制）
     *
     * @param tempDir 临时目录路径
     * @throws IOException 删除失败抛出异常
     */
    public static void deleteTempDirectory(Path tempDir) throws IOException {
        deleteTempDirectory(tempDir, MAX_DELETE_FILE_COUNT * 10);
    }

    /**
     * 批量删除临时目录（仅允许删除系统临时目录下的路径）
     *
     * @param tempDirs 临时目录列表
     * @throws IOException 删除过程中发生异常
     */
    public static void deleteTempDirectories(List<Path> tempDirs) throws IOException {
        if (tempDirs == null || tempDirs.isEmpty()) {
            return;
        }

        for (Path tempDir : tempDirs) {
            if (tempDir == null) {
                continue;
            }
            deleteTempDirectory(tempDir);
        }
    }

    /**
     * 判断一个目录或目录是否位于系统临时目录下（即 java.io.tmpdir 下）
     *
     * @param path 要检查的目录路径
     * @return true 表示为临时目录
     */
    public static boolean isInSystemTempDir(Path path) {
        if (path == null) {
            return false;
        }
        Path tempRoot = Paths.get(System.getProperty("java.io.tmpdir")).toAbsolutePath().normalize();
        Path target = path.toAbsolutePath().normalize();
        return target.startsWith(tempRoot);
    }

    /**
     * 删除文件或目录（支持递归删除目录，含安全文件数量限制）
     *
     * @param path               路径
     * @param maxDeleteFileCount 限制目录下最大文件数，超过该数不执行删除
     * @throws IOException 删除失败抛出异常
     */
    public static void deleteRecursively(Path path, Integer maxDeleteFileCount) throws IOException {
        if (Files.notExists(path)) {
            return;
        }

        if (Files.isDirectory(path)) {
            // 安全限制：限制目录下最大文件数
            long fileCount;
            try (Stream<Path> stream = Files.walk(path)) {
                fileCount = stream.count();
            }

            if (fileCount > maxDeleteFileCount) {
                throw new IOException("目录文件数量超过限制（" + maxDeleteFileCount + "），拒绝执行删除操作：" + path);
            }

            // 递归删除
            try (Stream<Path> walk = Files.walk(path)) {
                // 从子目录往上删
                walk.sorted((a, b) -> b.compareTo(a))
                        .forEach(p -> {
                            try {
                                Files.deleteIfExists(p);
                            } catch (IOException e) {
                                throw new UncheckedIOException("删除失败：" + p, e);
                            }
                        });
            } catch (UncheckedIOException e) {
                throw e.getCause();
            }
        } else {
            Files.deleteIfExists(path);
        }
    }

    /**
     * 删除文件或目录（支持递归删除目录，含安全文件数量限制）
     *
     * @param path 路径
     * @throws IOException 删除失败抛出异常
     */
    public static void deleteRecursively(Path path) throws IOException {
        deleteRecursively(path, MAX_DELETE_FILE_COUNT);
    }

    /**
     * 批量删除普通路径（可为文件或目录，目录将递归删除）
     *
     * @param paths 路径列表
     * @throws IOException 删除失败抛出异常
     */
    public static void deletePaths(List<Path> paths) throws IOException {
        if (paths == null || paths.isEmpty()) {
            return;
        }

        for (Path path : paths) {
            if (path == null || Files.notExists(path)) {
                continue;
            }
            deleteRecursively(path);
        }
    }

    /**
     * 重命名文件或目录
     *
     * @param source 源路径
     * @param target 目标路径
     * @throws IOException 重命名失败抛出异常
     */
    public static void rename(Path source, Path target) throws IOException {
        if (!Files.exists(source)) {
            throw new FileNotFoundException("源文件不存在：" + source);
        }
        if (Files.exists(target)) {
            throw new FileAlreadyExistsException("目标路径已存在：" + target);
        }
        Files.move(source, target);
    }

    /**
     * 复制文件或目录（支持递归复制目录）
     *
     * @param source    源路径
     * @param target    目标路径
     * @param overwrite 是否覆盖目标文件
     * @throws IOException 复制失败抛出异常
     */
    public static void copy(Path source, Path target, boolean overwrite) throws IOException {
        if (!Files.exists(source)) {
            throw new FileNotFoundException("源路径不存在：" + source);
        }

        CopyOption[] options = overwrite
                ? new CopyOption[]{StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES}
                : new CopyOption[]{StandardCopyOption.COPY_ATTRIBUTES};

        if (Files.isDirectory(source)) {
            // 创建目标目录
            Files.walk(source).forEach(path -> {
                try {
                    Path relative = source.relativize(path);
                    Path destPath = target.resolve(relative);
                    if (Files.isDirectory(path)) {
                        Files.createDirectories(destPath);
                    } else {
                        Files.copy(path, destPath, options);
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException("复制目录失败：" + path, e);
                }
            });
        } else {
            createDirectories(target.getParent());
            Files.copy(source, target, options);
        }
    }

    /**
     * 移动文件或目录（支持覆盖）
     *
     * @param source    源路径
     * @param target    目标路径
     * @param overwrite 是否覆盖目标文件
     * @throws IOException 移动失败抛出异常
     */
    public static void move(Path source, Path target, boolean overwrite) throws IOException {
        if (!Files.exists(source)) {
            throw new FileNotFoundException("源路径不存在：" + source);
        }

        CopyOption[] options = overwrite
                ? new CopyOption[]{StandardCopyOption.REPLACE_EXISTING}
                : new CopyOption[]{};

        createDirectories(target.getParent());
        Files.move(source, target, options);
    }

    /**
     * 读取文件内容为字符串
     *
     * @param path 文件路径
     * @return 文件内容
     * @throws IOException 读取失败抛出异常
     */
    public static String readAsString(Path path) throws IOException {
        if (!Files.exists(path)) {
            throw new FileNotFoundException("文件不存在：" + path);
        }
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    /**
     * 按行读取文件内容
     *
     * @param path 文件路径
     * @return 行列表
     * @throws IOException 读取失败抛出异常
     */
    public static List<String> readLines(Path path) throws IOException {
        if (!Files.exists(path)) {
            throw new FileNotFoundException("文件不存在：" + path);
        }
        return Files.readAllLines(path, StandardCharsets.UTF_8);
    }

    /**
     * 写入字符串内容到文件（覆盖模式）
     *
     * @param path    文件路径
     * @param content 内容
     * @throws IOException 写入失败抛出异常
     */
    public static void writeString(Path path, String content) throws IOException {
        if (path == null) {
            throw new IllegalArgumentException("文件路径不能为空");
        }
        createDirectories(path.getParent());
        Files.write(path, content.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * 追加内容到文件末尾
     *
     * @param path    文件路径
     * @param content 内容
     * @throws IOException 写入失败抛出异常
     */
    public static void appendString(Path path, String content) throws IOException {
        if (path == null) {
            throw new IllegalArgumentException("文件路径不能为空");
        }
        createDirectories(path.getParent());
        Files.write(path, content.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    // ============================== 批量文件输入输出操作 ==============================

    /**
     * 批量读取多个文件的内容（按行）
     *
     * @param paths 文件路径列表
     * @return 每个文件内容组成的列表（每个元素为一整个文件的所有行）
     * @throws IOException 读取失败抛出异常
     */
    public static List<List<String>> readAllFilesAsLines(List<Path> paths) throws IOException {
        if (paths == null || paths.isEmpty()) {
            return Collections.emptyList();
        }

        List<List<String>> result = new ArrayList<>();
        for (Path path : paths) {
            if (Files.exists(path) && Files.isRegularFile(path)) {
                result.add(Files.readAllLines(path, StandardCharsets.UTF_8));
            } else {
                result.add(Collections.emptyList());
            }
        }
        return result;
    }

    /**
     * 批量写入多个文件（覆盖模式）
     *
     * @param contentMap 写入映射（Key为文件路径，Value为写入内容）
     * @throws IOException 写入失败抛出异常
     */
    public static void writeMultipleFiles(Map<Path, String> contentMap) throws IOException {
        if (contentMap == null || contentMap.isEmpty()) {
            return;
        }

        for (Map.Entry<Path, String> entry : contentMap.entrySet()) {
            Path path = entry.getKey();
            String content = entry.getValue();
            if (path != null && content != null) {
                writeString(path, content);
            }
        }
    }

    /**
     * 批量写入多个文件（追加模式）
     *
     * @param contentMap 写入映射（Key为文件路径，Value为追加内容）
     * @throws IOException 写入失败抛出异常
     */
    public static void appendMultipleFiles(Map<Path, String> contentMap) throws IOException {
        if (contentMap == null || contentMap.isEmpty()) {
            return;
        }

        for (Map.Entry<Path, String> entry : contentMap.entrySet()) {
            Path path = entry.getKey();
            String content = entry.getValue();
            if (path != null && content != null) {
                appendString(path, content);
            }
        }
    }

    /**
     * 批量复制文件到指定目录
     *
     * @param sources   文件路径列表（不支持目录）
     * @param targetDir 目标目录（必须是目录）
     * @throws IOException 拷贝失败抛出异常
     */
    public static void copyMultipleFiles(List<Path> sources, Path targetDir) throws IOException {
        if (sources == null || sources.isEmpty()) {
            return;
        }
        if (targetDir == null) {
            throw new IllegalArgumentException("目标目录不能为空");
        }
        createDirectories(targetDir);

        for (Path source : sources) {
            if (Files.exists(source) && Files.isRegularFile(source)) {
                Path fileName = source.getFileName();
                if (fileName != null) {
                    Path targetPath = targetDir.resolve(fileName);
                    Files.copy(source, targetPath, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    /**
     * 打开文件的输入流
     *
     * @param path 文件路径
     * @return 输入流
     * @throws IOException 打开失败抛出异常
     */
    public static InputStream openInputStream(Path path) throws IOException {
        if (!Files.exists(path) || Files.isDirectory(path)) {
            throw new FileNotFoundException("文件不存在或不是普通文件：" + path);
        }
        return Files.newInputStream(path, StandardOpenOption.READ);
    }

    /**
     * 打开文件的输出流（覆盖模式），会自动创建父目录
     *
     * @param path 文件路径
     * @return 输出流
     * @throws IOException 打开失败抛出异常
     */
    public static OutputStream openOutputStream(Path path) throws IOException {
        if (path == null) {
            throw new IllegalArgumentException("文件路径不能为空");
        }
        createDirectories(path.getParent());
        return Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * 打开文件的输出流（追加模式），会自动创建父目录
     *
     * @param path 文件路径
     * @return 输出流（追加）
     * @throws IOException 打开失败抛出异常
     */
    public static OutputStream openAppendStream(Path path) throws IOException {
        if (path == null) {
            throw new IllegalArgumentException("文件路径不能为空");
        }
        createDirectories(path.getParent());
        return Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    /**
     * 打开缓冲读取器（BufferedReader）
     *
     * @param path 文件路径
     * @return BufferedReader
     * @throws IOException 打开失败抛出异常
     */
    public static BufferedReader openBufferedReader(Path path) throws IOException {
        return Files.newBufferedReader(path, StandardCharsets.UTF_8);
    }

    /**
     * 打开缓冲写入器（覆盖模式）
     *
     * @param path 文件路径
     * @return BufferedWriter
     * @throws IOException 打开失败抛出异常
     */
    public static BufferedWriter openBufferedWriter(Path path) throws IOException {
        createDirectories(path.getParent());
        return Files.newBufferedWriter(path, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * 拷贝一个输入流到输出流（使用缓冲区）
     *
     * @param in  输入流
     * @param out 输出流
     * @throws IOException 拷贝失败抛出异常
     */
    public static void copyStream(InputStream in, OutputStream out) throws IOException {
        Objects.requireNonNull(in, "输入流不能为空");
        Objects.requireNonNull(out, "输出流不能为空");

        byte[] buffer = new byte[BUFFER_SIZE];
        int len;
        while ((len = in.read(buffer)) != -1) {
            out.write(buffer, 0, len);
        }
        out.flush();
    }

    /**
     * 批量打开输入流
     *
     * @param paths 文件路径列表
     * @return 路径 -> 输入流 Map
     * @throws IOException 打开失败抛出异常
     */
    public static Map<Path, InputStream> openInputStreams(List<Path> paths) throws IOException {
        Map<Path, InputStream> streamMap = new LinkedHashMap<>();
        if (paths == null || paths.isEmpty()) {
            return streamMap;
        }

        for (Path path : paths) {
            if (Files.exists(path) && Files.isRegularFile(path)) {
                streamMap.put(path, openInputStream(path));
            } else {
                throw new FileNotFoundException("文件不存在或不是普通文件：" + path);
            }
        }
        return streamMap;
    }

    /**
     * 批量打开输出流（覆盖模式）
     *
     * @param paths 文件路径列表
     * @return 路径 -> 输出流 Map
     * @throws IOException 打开失败抛出异常
     */
    public static Map<Path, OutputStream> openOutputStreams(List<Path> paths) throws IOException {
        Map<Path, OutputStream> streamMap = new LinkedHashMap<>();
        if (paths == null || paths.isEmpty()) {
            return streamMap;
        }

        for (Path path : paths) {
            streamMap.put(path, openOutputStream(path));
        }
        return streamMap;
    }

    /**
     * 批量打开输出流（追加模式）
     *
     * @param paths 文件路径列表
     * @return 路径 -> 输出流 Map（追加）
     * @throws IOException 打开失败抛出异常
     */
    public static Map<Path, OutputStream> openAppendStreams(List<Path> paths) throws IOException {
        Map<Path, OutputStream> streamMap = new LinkedHashMap<>();
        if (paths == null || paths.isEmpty()) {
            return streamMap;
        }

        for (Path path : paths) {
            streamMap.put(path, openAppendStream(path));
        }
        return streamMap;
    }

    /**
     * 批量关闭流
     *
     * @param streams 输入/输出流集合
     */
    public static void closeStreams(Collection<? extends Closeable> streams) {
        if (streams == null) {
            return;
        }
        for (Closeable stream : streams) {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    // 日志可选：忽略单个关闭异常
                }
            }
        }
    }

    /**
     * 判断是否为空文件
     *
     * @param path 文件路径
     * @return true 表示不存在、不是普通文件或大小为 0
     */
    public static boolean isEmptyFile(Path path) {
        return !isFile(path) || getFileSize(path) <= 0L;
    }

    /**
     * 判断是否为非空文件
     *
     * @param path 文件路径
     * @return true 表示为普通文件且大小大于 0
     */
    public static boolean isNotEmptyFile(Path path) {
        return !isEmptyFile(path);
    }

    /**
     * 判断是否为空目录
     *
     * @param path 目录路径
     * @return true 表示不存在、不是目录或没有任何子项
     */
    public static boolean isEmptyDirectory(Path path) {
        if (!isDirectory(path)) {
            return true;
        }
        try (Stream<Path> stream = Files.list(path)) {
            return !stream.findFirst().isPresent();
        } catch (IOException e) {
            return true;
        }
    }

    /**
     * 判断路径是否可读
     *
     * @param path 路径
     * @return true 表示可读
     */
    public static boolean isReadable(Path path) {
        return exists(path) && Files.isReadable(path);
    }

    /**
     * 判断路径是否可写
     *
     * @param path 路径
     * @return true 表示可写
     */
    public static boolean isWritable(Path path) {
        return exists(path) && Files.isWritable(path);
    }

    /**
     * 判断路径是否可执行
     *
     * @param path 路径
     * @return true 表示可执行
     */
    public static boolean isExecutable(Path path) {
        return exists(path) && Files.isExecutable(path);
    }

    /**
     * 判断是否为隐藏文件或隐藏目录
     *
     * @param path 路径
     * @return true 表示隐藏
     */
    public static boolean isHidden(Path path) {
        if (!exists(path)) {
            return false;
        }
        try {
            return Files.isHidden(path);
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * 判断是否为符号链接
     *
     * @param path 路径
     * @return true 表示是符号链接
     */
    public static boolean isSymbolicLink(Path path) {
        return path != null && Files.isSymbolicLink(path);
    }

    /**
     * 获取父路径
     *
     * @param path 路径
     * @return 父路径，可能为 null
     */
    public static Path getParent(Path path) {
        return path == null ? null : path.getParent();
    }

    /**
     * 获取文件名，不带扩展名
     *
     * @param path 文件或目录路径
     * @return 不带扩展名的文件名
     */
    public static String getFileNameWithoutExtension(Path path) {
        if (path == null) {
            return "";
        }
        return getFileNameWithoutExtension(getFileName(path));
    }

    /**
     * 获取文件名，不带扩展名
     *
     * @param fileName 文件名
     * @return 不带扩展名的文件名
     */
    public static String getFileNameWithoutExtension(String fileName) {
        if (StringUtil.isBlank(fileName)) {
            return "";
        }
        int index = fileName.lastIndexOf(EXTENSION_DOT);
        return index > 0 ? fileName.substring(0, index) : fileName;
    }

    /**
     * 获取路径的最后修改时间
     *
     * @param path 路径
     * @return 最后修改时间，失败返回 null
     */
    public static FileTime getLastModifiedTime(Path path) {
        if (!exists(path)) {
            return null;
        }
        try {
            return Files.getLastModifiedTime(path);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * 设置最后修改时间
     *
     * @param path 路径
     * @param time 时间
     * @throws IOException 设置失败抛出异常
     */
    public static void setLastModifiedTime(Path path, FileTime time) throws IOException {
        if (path == null) {
            throw new IllegalArgumentException("路径不能为空");
        }
        if (time == null) {
            throw new IllegalArgumentException("时间不能为空");
        }
        if (Files.notExists(path)) {
            createFileIfNotExists(path);
        }
        Files.setLastModifiedTime(path, time);
    }

    /**
     * 触碰文件，不存在则创建，存在则更新最后修改时间
     *
     * @param path 文件路径
     * @throws IOException 操作失败抛出异常
     */
    public static void touch(Path path) throws IOException {
        if (path == null) {
            throw new IllegalArgumentException("文件路径不能为空");
        }
        createFileIfNotExists(path);
        setLastModifiedTime(path, FileTime.from(Instant.now()));
    }

    /**
     * 确保父目录存在
     *
     * @param path 文件路径
     * @throws IOException 创建失败抛出异常
     */
    public static void ensureParentDirectories(Path path) throws IOException {
        if (path == null) {
            return;
        }
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    /**
     * 确保目录存在，不存在则创建
     *
     * @param dirPath 目录路径
     * @throws IOException 创建失败抛出异常
     */
    public static void ensureDirectory(Path dirPath) throws IOException {
        if (dirPath == null) {
            throw new IllegalArgumentException("目录路径不能为空");
        }
        Files.createDirectories(dirPath);
    }

    /**
     * 确保文件存在，不存在则创建
     *
     * @param filePath 文件路径
     * @throws IOException 创建失败抛出异常
     */
    public static void ensureFile(Path filePath) throws IOException {
        createFileIfNotExists(filePath);
    }

    /**
     * 安全获取路径对象
     *
     * @param pathStr 路径字符串
     * @return Path 对象，空串返回 null
     */
    public static Path toPath(String pathStr) {
        if (StringUtil.isBlank(pathStr)) {
            return null;
        }
        return Paths.get(pathStr.trim());
    }

    /**
     * 规范化字符串路径
     *
     * @param pathStr 路径字符串
     * @return 规范化后的绝对路径字符串，空串返回空串
     */
    public static String normalizePath(String pathStr) {
        Path path = toPath(pathStr);
        return path == null ? "" : toAbsoluteNormalized(path).toString();
    }

    /**
     * 判断目录下是否存在符合条件的文件
     *
     * @param dir       目录
     * @param predicate 过滤条件
     * @param recursive 是否递归
     * @return true 表示存在符合条件的文件
     */
    public static boolean existsFile(Path dir, Predicate<Path> predicate, boolean recursive) {
        if (!isDirectory(dir)) {
            return false;
        }
        List<Path> files = listPaths(dir, recursive, Files::isRegularFile);
        if (CollectionUtil.isEmpty(files)) {
            return false;
        }
        if (predicate == null) {
            return !files.isEmpty();
        }
        for (Path file : files) {
            if (predicate.test(file)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取目录中的直接子路径
     *
     * @param dir 目录
     * @return 子路径列表
     */
    public static List<Path> listChildren(Path dir) {
        return listPaths(dir, false, p -> true);
    }

    /**
     * 获取目录中的文件列表
     *
     * @param dir 目录
     * @return 文件列表
     */
    public static List<Path> listFiles(Path dir) {
        return listPaths(dir, false, Files::isRegularFile);
    }

    /**
     * 获取目录中的文件列表
     *
     * @param dir       目录
     * @param recursive 是否递归
     * @return 文件列表
     */
    public static List<Path> listFiles(Path dir, boolean recursive) {
        return listPaths(dir, recursive, Files::isRegularFile);
    }

    /**
     * 获取目录中的目录列表
     *
     * @param dir 目录
     * @return 目录列表
     */
    public static List<Path> listDirectories(Path dir) {
        return listPaths(dir, false, Files::isDirectory);
    }

    /**
     * 获取目录中的目录列表
     *
     * @param dir       目录
     * @param recursive 是否递归
     * @return 目录列表
     */
    public static List<Path> listDirectories(Path dir, boolean recursive) {
        return listPaths(dir, recursive, Files::isDirectory);
    }

    /**
     * 获取目录中的路径列表
     *
     * @param dir       目录
     * @param recursive 是否递归
     * @param filter    过滤条件
     * @return 路径列表
     */
    public static List<Path> listPaths(Path dir, boolean recursive, Predicate<Path> filter) {
        if (!isDirectory(dir)) {
            return Collections.emptyList();
        }

        Predicate<Path> actualFilter = filter == null ? p -> true : filter;
        try {
            if (recursive) {
                try (Stream<Path> stream = Files.walk(dir)) {
                    return stream
                            .filter(path -> !Objects.equals(path, dir))
                            .filter(actualFilter)
                            .collect(Collectors.toList());
                }
            }
            try (Stream<Path> stream = Files.list(dir)) {
                return stream
                        .filter(actualFilter)
                        .collect(Collectors.toList());
            }
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    /**
     * 统计目录下文件数量
     *
     * @param dir       目录
     * @param recursive 是否递归
     * @return 文件数量
     */
    public static long countFiles(Path dir, boolean recursive) {
        if (!isDirectory(dir)) {
            return 0L;
        }
        try {
            if (recursive) {
                try (Stream<Path> stream = Files.walk(dir)) {
                    return stream.filter(Files::isRegularFile).count();
                }
            }
            try (Stream<Path> stream = Files.list(dir)) {
                return stream.filter(Files::isRegularFile).count();
            }
        } catch (IOException e) {
            return 0L;
        }
    }

    /**
     * 统计目录大小
     *
     * @param dir 目录
     * @return 目录总大小，失败返回 -1
     */
    public static long getDirectorySize(Path dir) {
        if (!isDirectory(dir)) {
            return -1L;
        }
        try (Stream<Path> stream = Files.walk(dir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .mapToLong(FileUtil::getFileSize)
                    .filter(size -> size >= 0L)
                    .sum();
        } catch (IOException e) {
            return -1L;
        }
    }

    /**
     * 统计目录总路径数量
     *
     * @param path 路径
     * @return 路径数量
     */
    public static long countPaths(Path path) {
        if (Files.notExists(path)) {
            return 0L;
        }
        try (Stream<Path> stream = Files.walk(path)) {
            return stream.count();
        } catch (IOException e) {
            return 0L;
        }
    }

    /**
     * 获取人类可读的文件大小
     *
     * @param bytes 字节数
     * @return 格式化后的大小
     */
    public static String getReadableFileSize(long bytes) {
        if (bytes < 0L) {
            return "unknown";
        }
        if (bytes < 1024L) {
            return bytes + " B";
        }
        double value = bytes;
        String[] units = new String[]{"KB", "MB", "GB", "TB", "PB"};
        int unitIndex = -1;
        while (value >= 1024L && unitIndex < units.length - 1) {
            value = value / 1024L;
            unitIndex++;
        }
        return String.format(Locale.ROOT, "%.2f %s", value, units[unitIndex]);
    }

    /**
     * 获取文件创建时间
     *
     * @param path 文件路径
     * @return 创建时间，失败返回 null
     */
    public static FileTime getCreationTime(Path path) {
        if (!exists(path)) {
            return null;
        }
        try {
            return (FileTime) Files.getAttribute(path, "basic:creationTime");
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取文件最后访问时间
     *
     * @param path 文件路径
     * @return 最后访问时间，失败返回 null
     */
    public static FileTime getLastAccessTime(Path path) {
        if (!exists(path)) {
            return null;
        }
        try {
            return (FileTime) Files.getAttribute(path, "basic:lastAccessTime");
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 读取文件的字节数组
     *
     * @param path 文件路径
     * @return 文件字节数组
     * @throws IOException 读取失败抛出异常
     */
    public static byte[] readBytes(Path path) throws IOException {
        if (!Files.exists(path)) {
            throw new FileNotFoundException("文件不存在：" + path);
        }
        return Files.readAllBytes(path);
    }

    /**
     * 读取文件内容为字符串，支持指定字符集
     *
     * @param path    文件路径
     * @param charset 字符集
     * @return 文件内容
     * @throws IOException 读取失败抛出异常
     */
    public static String readAsString(Path path, Charset charset) throws IOException {
        if (!Files.exists(path)) {
            throw new FileNotFoundException("文件不存在：" + path);
        }
        Charset actualCharset = charset == null ? StandardCharsets.UTF_8 : charset;
        return new String(Files.readAllBytes(path), actualCharset);
    }

    /**
     * 按行读取文件内容，支持指定字符集
     *
     * @param path    文件路径
     * @param charset 字符集
     * @return 行列表
     * @throws IOException 读取失败抛出异常
     */
    public static List<String> readLines(Path path, Charset charset) throws IOException {
        if (!Files.exists(path)) {
            throw new FileNotFoundException("文件不存在：" + path);
        }
        Charset actualCharset = charset == null ? StandardCharsets.UTF_8 : charset;
        return Files.readAllLines(path, actualCharset);
    }

    /**
     * 写入字节数组到文件
     *
     * @param path  文件路径
     * @param bytes 字节数组
     * @throws IOException 写入失败抛出异常
     */
    public static void writeBytes(Path path, byte[] bytes) throws IOException {
        if (path == null) {
            throw new IllegalArgumentException("文件路径不能为空");
        }
        ensureParentDirectories(path);
        Files.write(path, ObjectUtil.defaultIfNull(bytes, new byte[0]),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
    }

    /**
     * 追加字节数组到文件末尾
     *
     * @param path  文件路径
     * @param bytes 字节数组
     * @throws IOException 写入失败抛出异常
     */
    public static void appendBytes(Path path, byte[] bytes) throws IOException {
        if (path == null) {
            throw new IllegalArgumentException("文件路径不能为空");
        }
        ensureParentDirectories(path);
        Files.write(path, ObjectUtil.defaultIfNull(bytes, new byte[0]),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.WRITE);
    }

    /**
     * 写入字符串内容到文件，支持指定字符集
     *
     * @param path    文件路径
     * @param content 内容
     * @param charset 字符集
     * @throws IOException 写入失败抛出异常
     */
    public static void writeString(Path path, String content, Charset charset) throws IOException {
        if (path == null) {
            throw new IllegalArgumentException("文件路径不能为空");
        }
        ensureParentDirectories(path);
        Charset actualCharset = charset == null ? StandardCharsets.UTF_8 : charset;
        Files.write(path,
                ObjectUtil.defaultIfNull(content, "").getBytes(actualCharset),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
    }

    /**
     * 追加字符串内容到文件末尾，支持指定字符集
     *
     * @param path    文件路径
     * @param content 内容
     * @param charset 字符集
     * @throws IOException 写入失败抛出异常
     */
    public static void appendString(Path path, String content, Charset charset) throws IOException {
        if (path == null) {
            throw new IllegalArgumentException("文件路径不能为空");
        }
        ensureParentDirectories(path);
        Charset actualCharset = charset == null ? StandardCharsets.UTF_8 : charset;
        Files.write(path,
                ObjectUtil.defaultIfNull(content, "").getBytes(actualCharset),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.WRITE);
    }

    /**
     * 按行写入文件，支持指定字符集与追加模式
     *
     * @param path    文件路径
     * @param lines   行内容
     * @param charset 字符集
     * @param append  是否追加
     * @throws IOException 写入失败抛出异常
     */
    public static void writeLines(Path path, Collection<String> lines, Charset charset, boolean append) throws IOException {
        if (path == null) {
            throw new IllegalArgumentException("文件路径不能为空");
        }
        ensureParentDirectories(path);
        Charset actualCharset = charset == null ? StandardCharsets.UTF_8 : charset;

        List<String> actualLines = lines == null ? Collections.emptyList() : new ArrayList<>(lines);
        OpenOption[] options = append
                ? new OpenOption[]{StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.WRITE}
                : new OpenOption[]{StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE};
        Files.write(path, actualLines, actualCharset, options);
    }

    /**
     * 复制输入流到文件
     *
     * @param inputStream 输入流
     * @param target      目标文件
     * @throws IOException 复制失败抛出异常
     */
    public static void copyInputStreamToFile(InputStream inputStream, Path target) throws IOException {
        if (inputStream == null) {
            throw new IllegalArgumentException("输入流不能为空");
        }
        if (target == null) {
            throw new IllegalArgumentException("目标路径不能为空");
        }
        ensureParentDirectories(target);
        try (OutputStream outputStream = Files.newOutputStream(target,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            copyStream(inputStream, outputStream);
        }
    }

    /**
     * 复制文件到输出流
     *
     * @param source       源文件
     * @param outputStream 输出流
     * @throws IOException 复制失败抛出异常
     */
    public static void copyFileToOutputStream(Path source, OutputStream outputStream) throws IOException {
        if (!isFile(source)) {
            throw new FileNotFoundException("文件不存在或不是普通文件：" + source);
        }
        if (outputStream == null) {
            throw new IllegalArgumentException("输出流不能为空");
        }
        try (InputStream inputStream = Files.newInputStream(source, StandardOpenOption.READ)) {
            copyStream(inputStream, outputStream);
        }
    }

    /**
     * 复制文件到目录
     *
     * @param source    源文件
     * @param targetDir 目标目录
     * @param overwrite 是否覆盖
     * @throws IOException 复制失败抛出异常
     */
    public static void copyFileToDirectory(Path source, Path targetDir, boolean overwrite) throws IOException {
        if (!isFile(source)) {
            throw new FileNotFoundException("源文件不存在或不是普通文件：" + source);
        }
        if (targetDir == null) {
            throw new IllegalArgumentException("目标目录不能为空");
        }
        ensureDirectory(targetDir);
        Path fileName = source.getFileName();
        if (fileName == null) {
            throw new IOException("无法获取源文件名：" + source);
        }
        Path target = targetDir.resolve(fileName);
        if (overwrite) {
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
        } else {
            Files.copy(source, target, StandardCopyOption.COPY_ATTRIBUTES);
        }
    }

    /**
     * 复制目录内容到目标目录
     *
     * @param sourceDir 源目录
     * @param targetDir 目标目录
     * @param overwrite 是否覆盖
     * @throws IOException 复制失败抛出异常
     */
    public static void copyDirectory(Path sourceDir, Path targetDir, boolean overwrite) throws IOException {
        if (!isDirectory(sourceDir)) {
            throw new FileNotFoundException("源目录不存在或不是目录：" + sourceDir);
        }
        if (targetDir == null) {
            throw new IllegalArgumentException("目标目录不能为空");
        }
        ensureDirectory(targetDir);

        CopyOption[] options = overwrite
                ? new CopyOption[]{StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES}
                : new CopyOption[]{StandardCopyOption.COPY_ATTRIBUTES};

        try (Stream<Path> walk = Files.walk(sourceDir)) {
            walk.forEach(path -> {
                try {
                    Path relative = sourceDir.relativize(path);
                    Path destination = targetDir.resolve(relative);
                    if (Files.isDirectory(path)) {
                        Files.createDirectories(destination);
                    } else {
                        ensureParentDirectories(destination);
                        Files.copy(path, destination, options);
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException("复制目录失败：" + path, e);
                }
            });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    /**
     * 移动到目录下
     *
     * @param source    源文件或目录
     * @param targetDir 目标目录
     * @param overwrite 是否覆盖
     * @throws IOException 移动失败抛出异常
     */
    public static void moveToDirectory(Path source, Path targetDir, boolean overwrite) throws IOException {
        if (!Files.exists(source)) {
            throw new FileNotFoundException("源路径不存在：" + source);
        }
        if (targetDir == null) {
            throw new IllegalArgumentException("目标目录不能为空");
        }
        ensureDirectory(targetDir);

        Path fileName = source.getFileName();
        if (fileName == null) {
            throw new IOException("无法获取源文件名：" + source);
        }

        Path target = targetDir.resolve(fileName);
        move(source, target, overwrite);
    }

    /**
     * 删除目录下所有子项，但保留目录本身
     *
     * @param dir 目录路径
     * @throws IOException 删除失败抛出异常
     */
    public static void deleteChildren(Path dir) throws IOException {
        if (!isDirectory(dir)) {
            return;
        }
        try (Stream<Path> walk = Files.list(dir)) {
            walk.forEach(path -> {
                try {
                    deleteRecursively(path);
                } catch (IOException e) {
                    throw new UncheckedIOException("删除失败：" + path, e);
                }
            });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    /**
     * 清空目录内容，但保留目录本身
     *
     * @param dir 目录路径
     * @throws IOException 清理失败抛出异常
     */
    public static void emptyDirectory(Path dir) throws IOException {
        deleteChildren(dir);
    }

    /**
     * 计算文件 MD5
     *
     * @param path 文件路径
     * @return MD5 十六进制字符串
     * @throws IOException 计算失败抛出异常
     */
    public static String md5Hex(Path path) throws IOException {
        if (!isFile(path)) {
            throw new FileNotFoundException("文件不存在或不是普通文件：" + path);
        }

        try (InputStream inputStream = Files.newInputStream(path, StandardOpenOption.READ)) {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[BUFFER_SIZE];
            int len;

            while ((len = inputStream.read(buffer)) != -1) {
                md.update(buffer, 0, len);
            }

            byte[] digest = md.digest();
            return toHexString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("不支持的算法：MD5", e);
        }
    }

    /**
     * byte[] 转 16 进制字符串
     *
     * @param bytes 字节数组
     * @return 十六进制字符串
     */
    private static String toHexString(byte[] bytes) {
        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            String str = Integer.toHexString(0xff & b);
            if (str.length() == 1) {
                hex.append('0');
            }
            hex.append(str);
        }
        return hex.toString();
    }

    /**
     * 计算文件 SHA-256
     *
     * @param path 文件路径
     * @return SHA-256 十六进制字符串
     * @throws IOException 计算失败抛出异常
     */
    public static String sha256Hex(Path path) throws IOException {
        if (!isFile(path)) {
            throw new FileNotFoundException("文件不存在或不是普通文件：" + path);
        }

        try (InputStream inputStream = Files.newInputStream(path, StandardOpenOption.READ)) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[BUFFER_SIZE];
            int len;

            while ((len = inputStream.read(buffer)) != -1) {
                md.update(buffer, 0, len);
            }

            byte[] digest = md.digest();
            return toHexString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("不支持的算法：SHA-256", e);
        }
    }

    /**
     * 比较两个文件内容是否一致
     *
     * @param first  第一个文件
     * @param second 第二个文件
     * @return true 表示内容相同
     * @throws IOException 比较失败抛出异常
     */
    public static boolean contentEquals(Path first, Path second) throws IOException {
        if (first == null || second == null) {
            return false;
        }
        if (!isFile(first) || !isFile(second)) {
            return false;
        }

        // 文件大小不同直接返回 false（快速失败）
        if (Files.size(first) != Files.size(second)) {
            return false;
        }

        try (InputStream in1 = Files.newInputStream(first, StandardOpenOption.READ);
             InputStream in2 = Files.newInputStream(second, StandardOpenOption.READ)) {

            byte[] buffer1 = new byte[BUFFER_SIZE];
            byte[] buffer2 = new byte[BUFFER_SIZE];

            int len1;
            int len2;

            while ((len1 = in1.read(buffer1)) != -1) {
                len2 = in2.read(buffer2);

                // 理论上不会发生（因为 size 已相等），但仍做防御
                if (len1 != len2) {
                    return false;
                }

                for (int i = 0; i < len1; i++) {
                    if (buffer1[i] != buffer2[i]) {
                        return false;
                    }
                }
            }

            // 两个流都读完才算一致
            return in2.read() == -1;
        }
    }

    /**
     * 等待文件出现
     *
     * @param path               文件路径
     * @param timeoutMillis      超时时间
     * @param pollIntervalMillis 轮询间隔
     * @return true 表示在超时前出现
     */
    public static boolean waitForExists(Path path, long timeoutMillis, long pollIntervalMillis) {
        if (path == null) {
            return false;
        }
        long timeout = Math.max(timeoutMillis, 0L);
        long interval = Math.max(pollIntervalMillis, 1L);
        long start = System.currentTimeMillis();

        while (System.currentTimeMillis() - start <= timeout) {
            if (Files.exists(path)) {
                return true;
            }
            sleepQuietly(interval);
        }
        return Files.exists(path);
    }

    /**
     * 等待文件创建完成并可读
     *
     * @param path               文件路径
     * @param timeoutMillis      超时时间
     * @param pollIntervalMillis 轮询间隔
     * @return true 表示文件已可读
     */
    public static boolean waitForReadable(Path path, long timeoutMillis, long pollIntervalMillis) {
        if (path == null) {
            return false;
        }
        long timeout = Math.max(timeoutMillis, 0L);
        long interval = Math.max(pollIntervalMillis, 1L);
        long start = System.currentTimeMillis();

        while (System.currentTimeMillis() - start <= timeout) {
            if (isReadable(path)) {
                return true;
            }
            sleepQuietly(interval);
        }
        return isReadable(path);
    }

    /**
     * 等待文件大小稳定，常用于等待上传完成
     *
     * @param path               文件路径
     * @param stableMillis       文件大小连续稳定的时间
     * @param timeoutMillis      超时时间
     * @param pollIntervalMillis 轮询间隔
     * @return true 表示文件大小稳定
     */
    public static boolean waitForFileSizeStable(Path path, long stableMillis, long timeoutMillis, long pollIntervalMillis) {
        if (!exists(path) || !isFile(path)) {
            return false;
        }

        long timeout = Math.max(timeoutMillis, 0L);
        long interval = Math.max(pollIntervalMillis, 1L);
        long requiredStable = Math.max(stableMillis, interval);

        long start = System.currentTimeMillis();
        long lastSize = -1L;
        long stableStart = -1L;

        while (System.currentTimeMillis() - start <= timeout) {
            long currentSize = getFileSize(path);
            if (currentSize == lastSize && currentSize >= 0L) {
                if (stableStart < 0L) {
                    stableStart = System.currentTimeMillis();
                }
                if (System.currentTimeMillis() - stableStart >= requiredStable) {
                    return true;
                }
            } else {
                lastSize = currentSize;
                stableStart = -1L;
            }
            sleepQuietly(interval);
        }
        return false;
    }

    /**
     * 等待文件删除
     *
     * @param path               文件路径
     * @param timeoutMillis      超时时间
     * @param pollIntervalMillis 轮询间隔
     * @return true 表示在超时前删除
     */
    public static boolean waitForDelete(Path path, long timeoutMillis, long pollIntervalMillis) {
        if (path == null) {
            return false;
        }
        long timeout = Math.max(timeoutMillis, 0L);
        long interval = Math.max(pollIntervalMillis, 1L);
        long start = System.currentTimeMillis();

        while (System.currentTimeMillis() - start <= timeout) {
            if (Files.notExists(path)) {
                return true;
            }
            sleepQuietly(interval);
        }
        return Files.notExists(path);
    }

    /**
     * 等待指定目录出现
     *
     * @param path               目录路径
     * @param timeoutMillis      超时时间
     * @param pollIntervalMillis 轮询间隔
     * @return true 表示目录已出现
     */
    public static boolean waitForDirectory(Path path, long timeoutMillis, long pollIntervalMillis) {
        if (path == null) {
            return false;
        }
        long timeout = Math.max(timeoutMillis, 0L);
        long interval = Math.max(pollIntervalMillis, 1L);
        long start = System.currentTimeMillis();

        while (System.currentTimeMillis() - start <= timeout) {
            if (isDirectory(path)) {
                return true;
            }
            sleepQuietly(interval);
        }
        return isDirectory(path);
    }

    /**
     * 等待文件或目录可用
     *
     * @param path               路径
     * @param timeoutMillis      超时时间
     * @param pollIntervalMillis 轮询间隔
     * @return true 表示可用
     */
    public static boolean waitForAvailable(Path path, long timeoutMillis, long pollIntervalMillis) {
        return waitForExists(path, timeoutMillis, pollIntervalMillis);
    }

    /**
     * 创建系统默认位置的临时文件
     *
     * @return 临时文件路径
     * @throws IOException 创建失败抛出异常
     */
    public static Path createTempFile() throws IOException {
        return Files.createTempFile("temp_", ".tmp");
    }

    /**
     * 创建系统默认位置的临时文件，指定前缀和后缀
     *
     * @param prefix 前缀
     * @param suffix 后缀
     * @return 临时文件路径
     * @throws IOException 创建失败抛出异常
     */
    public static Path createTempFile(String prefix, String suffix) throws IOException {
        String actualPrefix = StringUtil.isBlank(prefix) ? "temp_" : prefix;
        String actualSuffix = StringUtil.isBlank(suffix) ? ".tmp" : suffix;
        return Files.createTempFile(actualPrefix, actualSuffix);
    }

    /**
     * 在指定目录中创建临时文件
     *
     * @param parentDir 父目录
     * @param prefix    前缀
     * @param suffix    后缀
     * @return 临时文件路径
     * @throws IOException 创建失败抛出异常
     */
    public static Path createTempFile(Path parentDir, String prefix, String suffix) throws IOException {
        if (parentDir == null) {
            throw new IllegalArgumentException("父级目录不能为空");
        }
        ensureDirectory(parentDir);
        String actualPrefix = StringUtil.isBlank(prefix) ? "temp_" : prefix;
        String actualSuffix = StringUtil.isBlank(suffix) ? ".tmp" : suffix;
        return Files.createTempFile(parentDir, actualPrefix, actualSuffix);
    }

    /**
     * 获取文件后缀对应的 MIME 类型，优先使用已知映射
     *
     * @param path 文件路径
     * @return MIME 类型
     */
    public static String getMimeType(Path path) {
        if (path == null) {
            return "application/octet-stream";
        }
        return getMimeType(getFileExtension(path));
    }

    /**
     * 获取文件类别
     *
     * @param path 文件路径
     * @return 文件分类
     */
    public static String getMimeCategory(Path path) {
        if (path == null) {
            return "unknown";
        }
        return getMimeCategory(getFileExtension(path));
    }

    /**
     * 获取文件是否存在且是普通文件
     *
     * @param path 文件路径
     * @return true 表示可用文件
     */
    public static boolean existsRegularFile(Path path) {
        return isFile(path);
    }

    /**
     * 获取文件是否存在且是目录
     *
     * @param path 路径
     * @return true 表示可用目录
     */
    public static boolean existsDirectory(Path path) {
        return isDirectory(path);
    }

    /**
     * 批量关闭流，忽略异常
     *
     * @param streams 输入/输出流集合
     */
    public static void closeStreamsQuietly(Collection<? extends Closeable> streams) {
        if (streams == null || streams.isEmpty()) {
            return;
        }

        for (Closeable stream : streams) {
            if (stream == null) {
                continue;
            }
            try {
                stream.close();
            } catch (IOException e) {
                // 忽略异常
            }
        }
    }

    /**
     * 批量关闭流，并尽量保留调用链可读性
     *
     * @param streams 输入/输出流集合
     * @param quiet   是否静默关闭（true 忽略异常，false 抛出异常）
     */
    public static void closeStreams(Collection<? extends Closeable> streams, boolean quiet) {
        if (streams == null || streams.isEmpty()) {
            return;
        }

        for (Closeable stream : streams) {
            if (stream == null) {
                continue;
            }

            if (quiet) {
                try {
                    stream.close();
                } catch (IOException e) {
                    // 忽略异常
                }
            } else {
                try {
                    stream.close();
                } catch (IOException e) {
                    throw new UncheckedIOException("关闭流失败", e);
                }
            }
        }
    }

    /**
     * 睡眠指定时间，忽略中断
     *
     * @param millis 毫秒数
     */
    private static void sleepQuietly(long millis) {
        try {
            TimeUnit.MILLISECONDS.sleep(Math.max(millis, 1L));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}

