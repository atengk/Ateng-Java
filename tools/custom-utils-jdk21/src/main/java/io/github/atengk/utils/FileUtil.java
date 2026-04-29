package io.github.atengk.utils;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.*;

/**
 * JDK 原生文件工具类，提供路径、读写、复制、删除、遍历、安全校验、压缩、哈希等常用能力。
 *
 * @author Ateng
 * @since 2026-04-29
 */
public final class FileUtil {

    private static final int BUFFER_SIZE = 8192;
    private static final String ILLEGAL_FILE_NAME_PATTERN = "[\\\\/:*?\"<>|\\p{Cntrl}]+";
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final DateTimeFormatter DATE_DIR_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private static final Map<String, String> MIME_FALLBACKS = createMimeFallbacks();

    private FileUtil() {
        throw new UnsupportedOperationException("FileUtil 不允许实例化");
    }

    /**
     * 将字符串路径转换为 Path。
     *
     * @param path 字符串路径
     * @return Path 对象
     */
    public static Path toPath(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("路径不能为空");
        }
        return Paths.get(path);
    }

    /**
     * 将 Path 转换为 File。
     *
     * @param path 文件路径
     * @return File 对象
     */
    public static File toFile(Path path) {
        return requireNonNullPath(path).toFile();
    }

    /**
     * 标准化字符串路径。
     *
     * @param path 字符串路径
     * @return 标准化后的路径字符串
     */
    public static String normalizePath(String path) {
        return toPath(path).normalize().toString();
    }

    /**
     * 拼接多个路径片段。
     *
     * @param first 第一个路径片段
     * @param more 其他路径片段
     * @return 拼接后的路径
     */
    public static Path joinPath(String first, String... more) {
        if (first == null || first.isBlank()) {
            throw new IllegalArgumentException("第一个路径片段不能为空");
        }
        return Paths.get(first, more == null ? new String[0] : more).normalize();
    }

    /**
     * 转换为绝对路径。
     *
     * @param path 文件路径
     * @return 绝对路径
     */
    public static Path toAbsolutePath(Path path) {
        return requireNonNullPath(path).toAbsolutePath().normalize();
    }

    /**
     * 转换为规范路径。
     *
     * @param path 文件路径
     * @return 规范路径
     * @throws IOException 路径解析失败时抛出
     */
    public static Path toCanonicalPath(Path path) throws IOException {
        return requireNonNullPath(path).toRealPath();
    }

    /**
     * 获取父级路径。
     *
     * @param path 文件路径
     * @return 父级路径，不存在时返回 null
     */
    public static Path getParentPath(Path path) {
        return requireNonNullPath(path).getParent();
    }

    /**
     * 获取文件名。
     *
     * @param path 文件路径
     * @return 文件名
     */
    public static String getFileName(Path path) {
        Path fileName = requireNonNullPath(path).getFileName();
        return fileName == null ? "" : fileName.toString();
    }

    /**
     * 获取不带扩展名的文件名。
     *
     * @param path 文件路径
     * @return 不带扩展名的文件名
     */
    public static String getBaseName(Path path) {
        String fileName = getFileName(path);
        int index = fileName.lastIndexOf('.');
        return index <= 0 ? fileName : fileName.substring(0, index);
    }

    /**
     * 获取文件扩展名，不包含点号。
     *
     * @param path 文件路径
     * @return 小写扩展名，不存在时返回空字符串
     */
    public static String getExtName(Path path) {
        String fileName = getFileName(path);
        int index = fileName.lastIndexOf('.');
        if (index < 0 || index == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    /**
     * 修改文件扩展名。
     *
     * @param path 文件路径
     * @param extName 新扩展名
     * @return 修改扩展名后的路径
     */
    public static Path changeExtName(Path path, String extName) {
        requireNonNullPath(path);
        String cleanExt = normalizeExtName(extName);
        String newName = getBaseName(path) + (cleanExt.isEmpty() ? "" : "." + cleanExt);
        return replaceFileName(path, newName);
    }

    /**
     * 移除文件扩展名。
     *
     * @param path 文件路径
     * @return 移除扩展名后的路径
     */
    public static Path removeExtName(Path path) {
        return replaceFileName(path, getBaseName(path));
    }

    /**
     * 判断目标路径是否位于基础路径下。
     *
     * @param basePath 基础路径
     * @param targetPath 目标路径
     * @return 位于基础路径下返回 true
     */
    public static boolean startsWithPath(Path basePath, Path targetPath) {
        Path base = toAbsolutePath(basePath);
        Path target = toAbsolutePath(targetPath);
        return target.startsWith(base);
    }

    /**
     * 判断两个路径是否指向相同路径。
     *
     * @param first 第一个路径
     * @param second 第二个路径
     * @return 路径相同返回 true
     */
    public static boolean isSamePath(Path first, Path second) {
        return toAbsolutePath(first).equals(toAbsolutePath(second));
    }

    /**
     * 判断文件或目录是否存在。
     *
     * @param path 文件路径
     * @return 存在返回 true
     */
    public static boolean exists(Path path) {
        return path != null && Files.exists(path);
    }

    /**
     * 判断文件或目录是否不存在。
     *
     * @param path 文件路径
     * @return 不存在返回 true
     */
    public static boolean notExists(Path path) {
        return path == null || Files.notExists(path);
    }

    /**
     * 判断路径是否为普通文件。
     *
     * @param path 文件路径
     * @return 是普通文件返回 true
     */
    public static boolean isFile(Path path) {
        return path != null && Files.isRegularFile(path);
    }

    /**
     * 判断路径是否为目录。
     *
     * @param path 文件路径
     * @return 是目录返回 true
     */
    public static boolean isDir(Path path) {
        return path != null && Files.isDirectory(path);
    }

    /**
     * 判断路径是否可读。
     *
     * @param path 文件路径
     * @return 可读返回 true
     */
    public static boolean isReadable(Path path) {
        return path != null && Files.isReadable(path);
    }

    /**
     * 判断路径是否可写。
     *
     * @param path 文件路径
     * @return 可写返回 true
     */
    public static boolean isWritable(Path path) {
        return path != null && Files.isWritable(path);
    }

    /**
     * 判断路径是否可执行。
     *
     * @param path 文件路径
     * @return 可执行返回 true
     */
    public static boolean isExecutable(Path path) {
        return path != null && Files.isExecutable(path);
    }

    /**
     * 判断路径是否为隐藏文件。
     *
     * @param path 文件路径
     * @return 隐藏文件返回 true
     * @throws IOException 读取属性失败时抛出
     */
    public static boolean isHidden(Path path) throws IOException {
        return Files.isHidden(requireNonNullPath(path));
    }

    /**
     * 判断文件是否为空文件。
     *
     * @param path 文件路径
     * @return 空文件返回 true
     * @throws IOException 读取大小失败时抛出
     */
    public static boolean isEmptyFile(Path path) throws IOException {
        checkIsFile(path);
        return Files.size(path) == 0;
    }

    /**
     * 判断目录是否为空。
     *
     * @param dir 目录路径
     * @return 空目录返回 true
     * @throws IOException 读取目录失败时抛出
     */
    public static boolean isEmptyDir(Path dir) throws IOException {
        checkIsDir(dir);
        try (Stream<Path> stream = Files.list(dir)) {
            return stream.findAny().isEmpty();
        }
    }

    /**
     * 判断路径是否为符号链接。
     *
     * @param path 文件路径
     * @return 是符号链接返回 true
     */
    public static boolean isSymbolicLink(Path path) {
        return path != null && Files.isSymbolicLink(path);
    }

    /**
     * 判断两个路径是否指向同一文件。
     *
     * @param first 第一个路径
     * @param second 第二个路径
     * @return 同一文件返回 true
     * @throws IOException 判断失败时抛出
     */
    public static boolean isSameFile(Path first, Path second) throws IOException {
        return Files.isSameFile(requireNonNullPath(first), requireNonNullPath(second));
    }

    /**
     * 判断文件名是否合法。
     *
     * @param fileName 文件名
     * @return 合法返回 true
     */
    public static boolean isValidFileName(String fileName) {
        return fileName != null && !fileName.isBlank() && fileName.equals(sanitizeFileName(fileName)) && !fileName.equals(".") && !fileName.equals("..");
    }

    /**
     * 判断目标路径是否安全地位于基础目录内。
     *
     * @param baseDir 基础目录
     * @param target 目标路径
     * @return 安全返回 true
     */
    public static boolean isSafePath(Path baseDir, Path target) {
        if (baseDir == null || target == null) {
            return false;
        }
        try {
            Path base = baseDir.toRealPath();
            Path resolved = target.toAbsolutePath().normalize();
            return resolved.startsWith(base);
        } catch (IOException ex) {
            return false;
        }
    }

    /**
     * 创建单级目录。
     *
     * @param dir 目录路径
     * @return 创建后的目录路径
     * @throws IOException 创建失败时抛出
     */
    public static Path mkdir(Path dir) throws IOException {
        return Files.createDirectory(requireNonNullPath(dir));
    }

    /**
     * 创建多级目录。
     *
     * @param dir 目录路径
     * @return 创建后的目录路径
     * @throws IOException 创建失败时抛出
     */
    public static Path mkdirs(Path dir) throws IOException {
        return Files.createDirectories(requireNonNullPath(dir));
    }

    /**
     * 确保目录存在。
     *
     * @param dir 目录路径
     * @return 目录路径
     * @throws IOException 创建失败时抛出
     */
    public static Path ensureDir(Path dir) throws IOException {
        requireNonNullPath(dir);
        if (Files.exists(dir) && !Files.isDirectory(dir)) {
            throw new FileAlreadyExistsException(dir.toString(), null, "路径已存在但不是目录");
        }
        return Files.createDirectories(dir);
    }

    /**
     * 确保文件父目录存在。
     *
     * @param path 文件路径
     * @return 父目录路径，文件无父目录时返回 null
     * @throws IOException 创建失败时抛出
     */
    public static Path ensureParentDir(Path path) throws IOException {
        Path parent = requireNonNullPath(path).toAbsolutePath().normalize().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        return parent;
    }

    /**
     * 创建文件。
     *
     * @param path 文件路径
     * @return 创建后的文件路径
     * @throws IOException 创建失败时抛出
     */
    public static Path createFile(Path path) throws IOException {
        ensureParentDir(path);
        return Files.createFile(requireNonNullPath(path));
    }

    /**
     * 文件不存在时创建文件。
     *
     * @param path 文件路径
     * @return 文件路径
     * @throws IOException 创建失败时抛出
     */
    public static Path createFileIfAbsent(Path path) throws IOException {
        ensureParentDir(path);
        if (Files.notExists(path)) {
            Files.createFile(path);
        }
        return path;
    }

    /**
     * 创建文件或更新最后修改时间。
     *
     * @param path 文件路径
     * @return 文件路径
     * @throws IOException 操作失败时抛出
     */
    public static Path touch(Path path) throws IOException {
        requireNonNullPath(path);
        if (Files.notExists(path)) {
            createFile(path);
        } else {
            Files.setLastModifiedTime(path, FileTime.from(Instant.now()));
        }
        return path;
    }

    /**
     * 在系统临时目录创建临时文件。
     *
     * @param prefix 文件名前缀
     * @param suffix 文件名后缀
     * @return 临时文件路径
     * @throws IOException 创建失败时抛出
     */
    public static Path createTempFile(String prefix, String suffix) throws IOException {
        return Files.createTempFile(prefix, suffix);
    }

    /**
     * 在指定目录创建临时文件。
     *
     * @param dir 目录路径
     * @param prefix 文件名前缀
     * @param suffix 文件名后缀
     * @return 临时文件路径
     * @throws IOException 创建失败时抛出
     */
    public static Path createTempFile(Path dir, String prefix, String suffix) throws IOException {
        ensureDir(dir);
        return Files.createTempFile(dir, prefix, suffix);
    }

    /**
     * 在系统临时目录创建临时目录。
     *
     * @param prefix 目录名前缀
     * @return 临时目录路径
     * @throws IOException 创建失败时抛出
     */
    public static Path createTempDir(String prefix) throws IOException {
        return Files.createTempDirectory(prefix);
    }

    /**
     * 在指定目录创建临时目录。
     *
     * @param dir 父目录路径
     * @param prefix 目录名前缀
     * @return 临时目录路径
     * @throws IOException 创建失败时抛出
     */
    public static Path createTempDir(Path dir, String prefix) throws IOException {
        ensureDir(dir);
        return Files.createTempDirectory(dir, prefix);
    }

    /**
     * 创建多级目录。
     *
     * @param dir 目录路径
     * @return 创建后的目录路径
     * @throws IOException 创建失败时抛出
     */
    public static Path createDirectories(Path dir) throws IOException {
        return mkdirs(dir);
    }

    /**
     * 删除文件或空目录。
     *
     * @param path 文件路径
     * @throws IOException 删除失败时抛出
     */
    public static void delete(Path path) throws IOException {
        Files.delete(requireNonNullPath(path));
    }

    /**
     * 存在时删除文件或空目录。
     *
     * @param path 文件路径
     * @return 删除成功返回 true，不存在返回 false
     * @throws IOException 删除失败时抛出
     */
    public static boolean deleteIfExists(Path path) throws IOException {
        return path != null && Files.deleteIfExists(path);
    }

    /**
     * 静默删除文件或空目录。
     *
     * @param path 文件路径
     * @return 删除成功返回 true，否则返回 false
     */
    public static boolean deleteQuietly(Path path) {
        try {
            return deleteIfExists(path);
        } catch (IOException | RuntimeException ex) {
            return false;
        }
    }

    /**
     * 删除普通文件。
     *
     * @param file 文件路径
     * @throws IOException 删除失败时抛出
     */
    public static void deleteFile(Path file) throws IOException {
        checkIsFile(file);
        Files.delete(file);
    }

    /**
     * 删除空目录。
     *
     * @param dir 目录路径
     * @throws IOException 删除失败时抛出
     */
    public static void deleteDir(Path dir) throws IOException {
        checkIsDir(dir);
        Files.delete(dir);
    }

    /**
     * 递归删除文件或目录。
     *
     * @param path 文件或目录路径
     * @throws IOException 删除失败时抛出
     */
    public static void deleteRecursively(Path path) throws IOException {
        requireNonNullPath(path);
        if (Files.notExists(path)) {
            return;
        }
        try (Stream<Path> stream = Files.walk(path)) {
            List<Path> paths = stream.sorted(Comparator.reverseOrder()).toList();
            for (Path item : paths) {
                Files.deleteIfExists(item);
            }
        }
    }

    /**
     * 清空目录但保留目录本身。
     *
     * @param dir 目录路径
     * @throws IOException 清理失败时抛出
     */
    public static void cleanDir(Path dir) throws IOException {
        checkIsDir(dir);
        try (Stream<Path> stream = Files.list(dir)) {
            for (Path item : stream.toList()) {
                deleteRecursively(item);
            }
        }
    }

    /**
     * 删除空目录。
     *
     * @param dir 目录路径
     * @return 删除成功返回 true
     * @throws IOException 删除失败时抛出
     */
    public static boolean deleteEmptyDir(Path dir) throws IOException {
        checkIsDir(dir);
        if (!isEmptyDir(dir)) {
            throw new DirectoryNotEmptyException(dir.toString());
        }
        return Files.deleteIfExists(dir);
    }

    /**
     * JVM 退出时删除文件或目录。
     *
     * @param path 文件路径
     */
    public static void deleteOnExit(Path path) {
        requireNonNullPath(path).toFile().deleteOnExit();
    }

    /**
     * 在基础目录范围内安全删除目标路径。
     *
     * @param baseDir 基础目录
     * @param target 目标路径
     * @return 删除成功返回 true，不存在返回 false
     * @throws IOException 路径不安全或删除失败时抛出
     */
    public static boolean safeDelete(Path baseDir, Path target) throws IOException {
        requireSafePath(baseDir, target);
        return Files.exists(target) && deleteQuietly(target);
    }

    /**
     * 复制文件或目录。
     *
     * @param source 源路径
     * @param target 目标路径
     * @return 目标路径
     * @throws IOException 复制失败时抛出
     */
    public static Path copy(Path source, Path target) throws IOException {
        if (Files.isDirectory(requireNonNullPath(source))) {
            return copyDir(source, target);
        }
        return copyFile(source, target);
    }

    /**
     * 复制文件。
     *
     * @param source 源文件
     * @param target 目标文件
     * @return 目标文件
     * @throws IOException 复制失败时抛出
     */
    public static Path copyFile(Path source, Path target) throws IOException {
        checkIsFile(source);
        ensureParentDir(target);
        return Files.copy(source, requireNonNullPath(target), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
    }

    /**
     * 复制目录。
     *
     * @param sourceDir 源目录
     * @param targetDir 目标目录
     * @return 目标目录
     * @throws IOException 复制失败时抛出
     */
    public static Path copyDir(Path sourceDir, Path targetDir) throws IOException {
        checkIsDir(sourceDir);
        requireNonNullPath(targetDir);
        try (Stream<Path> stream = Files.walk(sourceDir)) {
            for (Path source : stream.toList()) {
                Path target = targetDir.resolve(sourceDir.relativize(source));
                if (Files.isDirectory(source)) {
                    ensureDir(target);
                } else {
                    ensureParentDir(target);
                    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                }
            }
        }
        return targetDir;
    }

    /**
     * 目标不存在时复制文件或目录。
     *
     * @param source 源路径
     * @param target 目标路径
     * @return 目标路径
     * @throws IOException 复制失败时抛出
     */
    public static Path copyIfAbsent(Path source, Path target) throws IOException {
        if (Files.exists(requireNonNullPath(target))) {
            return target;
        }
        return copy(source, target);
    }

    /**
     * 覆盖复制文件或目录。
     *
     * @param source 源路径
     * @param target 目标路径
     * @return 目标路径
     * @throws IOException 复制失败时抛出
     */
    public static Path copyOverwrite(Path source, Path target) throws IOException {
        if (Files.exists(requireNonNullPath(target))) {
            deleteRecursively(target);
        }
        return copy(source, target);
    }

    /**
     * 移动文件或目录。
     *
     * @param source 源路径
     * @param target 目标路径
     * @return 目标路径
     * @throws IOException 移动失败时抛出
     */
    public static Path move(Path source, Path target) throws IOException {
        ensureParentDir(target);
        return Files.move(requireNonNullPath(source), requireNonNullPath(target), StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * 移动文件。
     *
     * @param source 源文件
     * @param target 目标文件
     * @return 目标文件
     * @throws IOException 移动失败时抛出
     */
    public static Path moveFile(Path source, Path target) throws IOException {
        checkIsFile(source);
        return move(source, target);
    }

    /**
     * 移动目录。
     *
     * @param sourceDir 源目录
     * @param targetDir 目标目录
     * @return 目标目录
     * @throws IOException 移动失败时抛出
     */
    public static Path moveDir(Path sourceDir, Path targetDir) throws IOException {
        checkIsDir(sourceDir);
        return move(sourceDir, targetDir);
    }

    /**
     * 重命名文件或目录。
     *
     * @param source 源路径
     * @param newName 新名称
     * @return 重命名后的路径
     * @throws IOException 重命名失败时抛出
     */
    public static Path rename(Path source, String newName) throws IOException {
        requireNonNullPath(source);
        checkFileNameSafe(newName);
        Path parent = source.getParent();
        Path target = parent == null ? Paths.get(newName) : parent.resolve(newName);
        return move(source, target);
    }

    /**
     * 替换目标文件。
     *
     * @param source 源文件
     * @param target 目标文件
     * @return 目标文件
     * @throws IOException 替换失败时抛出
     */
    public static Path replace(Path source, Path target) throws IOException {
        checkIsFile(source);
        ensureParentDir(target);
        return Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    /**
     * 备份文件。
     *
     * @param source 源文件
     * @param backupPath 备份路径
     * @return 备份文件路径
     * @throws IOException 备份失败时抛出
     */
    public static Path backup(Path source, Path backupPath) throws IOException {
        return copyFile(source, backupPath);
    }

    /**
     * 使用时间戳备份文件。
     *
     * @param source 源文件
     * @return 备份文件路径
     * @throws IOException 备份失败时抛出
     */
    public static Path backupWithTimestamp(Path source) throws IOException {
        checkIsFile(source);
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        String fileName = getBaseName(source) + "-" + timestamp;
        String ext = getExtName(source);
        Path backup = replaceFileName(source, ext.isEmpty() ? fileName : fileName + "." + ext);
        return backup(source, backup);
    }

    /**
     * 读取文本内容。
     *
     * @param path 文件路径
     * @param charset 字符集
     * @return 文本内容
     * @throws IOException 读取失败时抛出
     */
    public static String readString(Path path, Charset charset) throws IOException {
        return Files.readString(requireFile(path), requireNonNullCharset(charset));
    }

    /**
     * 按 UTF-8 读取文本内容。
     *
     * @param path 文件路径
     * @return 文本内容
     * @throws IOException 读取失败时抛出
     */
    public static String readUtf8String(Path path) throws IOException {
        return readString(path, StandardCharsets.UTF_8);
    }

    /**
     * 按行读取文本内容。
     *
     * @param path 文件路径
     * @param charset 字符集
     * @return 文本行列表
     * @throws IOException 读取失败时抛出
     */
    public static List<String> readLines(Path path, Charset charset) throws IOException {
        return Files.readAllLines(requireFile(path), requireNonNullCharset(charset));
    }

    /**
     * 按 UTF-8 按行读取文本内容。
     *
     * @param path 文件路径
     * @return 文本行列表
     * @throws IOException 读取失败时抛出
     */
    public static List<String> readUtf8Lines(Path path) throws IOException {
        return readLines(path, StandardCharsets.UTF_8);
    }

    /**
     * 写入文本内容。
     *
     * @param path 文件路径
     * @param content 文本内容
     * @param charset 字符集
     * @return 文件路径
     * @throws IOException 写入失败时抛出
     */
    public static Path writeString(Path path, String content, Charset charset) throws IOException {
        ensureParentDir(path);
        return Files.writeString(requireNonNullPath(path), Objects.requireNonNullElse(content, ""), requireNonNullCharset(charset), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
    }

    /**
     * 按 UTF-8 写入文本内容。
     *
     * @param path 文件路径
     * @param content 文本内容
     * @return 文件路径
     * @throws IOException 写入失败时抛出
     */
    public static Path writeUtf8String(Path path, String content) throws IOException {
        return writeString(path, content, StandardCharsets.UTF_8);
    }

    /**
     * 写入多行文本。
     *
     * @param path 文件路径
     * @param lines 文本行
     * @param charset 字符集
     * @return 文件路径
     * @throws IOException 写入失败时抛出
     */
    public static Path writeLines(Path path, Iterable<String> lines, Charset charset) throws IOException {
        ensureParentDir(path);
        return Files.write(requireNonNullPath(path), Objects.requireNonNull(lines, "文本行不能为空"), requireNonNullCharset(charset), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
    }

    /**
     * 追加文本内容。
     *
     * @param path 文件路径
     * @param content 文本内容
     * @param charset 字符集
     * @return 文件路径
     * @throws IOException 追加失败时抛出
     */
    public static Path appendString(Path path, String content, Charset charset) throws IOException {
        ensureParentDir(path);
        return Files.writeString(requireNonNullPath(path), Objects.requireNonNullElse(content, ""), requireNonNullCharset(charset), StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.WRITE);
    }

    /**
     * 追加多行文本。
     *
     * @param path 文件路径
     * @param lines 文本行
     * @param charset 字符集
     * @return 文件路径
     * @throws IOException 追加失败时抛出
     */
    public static Path appendLines(Path path, Iterable<String> lines, Charset charset) throws IOException {
        ensureParentDir(path);
        return Files.write(requireNonNullPath(path), Objects.requireNonNull(lines, "文本行不能为空"), requireNonNullCharset(charset), StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.WRITE);
    }

    /**
     * 创建缓冲字符输入流。
     *
     * @param path 文件路径
     * @param charset 字符集
     * @return 缓冲字符输入流
     * @throws IOException 创建失败时抛出
     */
    public static BufferedReader newBufferedReader(Path path, Charset charset) throws IOException {
        return Files.newBufferedReader(requireFile(path), requireNonNullCharset(charset));
    }

    /**
     * 创建缓冲字符输出流。
     *
     * @param path 文件路径
     * @param charset 字符集
     * @param options 打开选项
     * @return 缓冲字符输出流
     * @throws IOException 创建失败时抛出
     */
    public static BufferedWriter newBufferedWriter(Path path, Charset charset, OpenOption... options) throws IOException {
        ensureParentDir(path);
        OpenOption[] openOptions = options == null || options.length == 0
                ? new OpenOption[]{StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE}
                : options;
        return Files.newBufferedWriter(requireNonNullPath(path), requireNonNullCharset(charset), openOptions);
    }

    /**
     * 读取第一行文本。
     *
     * @param path 文件路径
     * @param charset 字符集
     * @return 第一行，不存在内容时返回 Optional.empty
     * @throws IOException 读取失败时抛出
     */
    public static Optional<String> readFirstLine(Path path, Charset charset) throws IOException {
        try (BufferedReader reader = newBufferedReader(path, charset)) {
            return Optional.ofNullable(reader.readLine());
        }
    }

    /**
     * 读取最后指定行数的文本。
     *
     * @param path 文件路径
     * @param lineCount 行数
     * @param charset 字符集
     * @return 最后指定行数的文本列表
     * @throws IOException 读取失败时抛出
     */
    public static List<String> readLastLines(Path path, int lineCount, Charset charset) throws IOException {
        if (lineCount < 0) {
            throw new IllegalArgumentException("行数不能小于 0");
        }
        List<String> lines = readLines(path, charset);
        if (lineCount == 0 || lines.isEmpty()) {
            return List.of();
        }
        return lines.subList(Math.max(0, lines.size() - lineCount), lines.size());
    }

    /**
     * 替换文本文件中的内容。
     *
     * @param path 文件路径
     * @param target 目标文本
     * @param replacement 替换文本
     * @param charset 字符集
     * @return 文件路径
     * @throws IOException 替换失败时抛出
     */
    public static Path replaceText(Path path, String target, String replacement, Charset charset) throws IOException {
        if (target == null || target.isEmpty()) {
            throw new IllegalArgumentException("目标文本不能为空");
        }
        String content = readString(path, charset).replace(target, Objects.requireNonNullElse(replacement, ""));
        return writeString(path, content, charset);
    }

    /**
     * 读取文件字节数组。
     *
     * @param path 文件路径
     * @return 字节数组
     * @throws IOException 读取失败时抛出
     */
    public static byte[] readBytes(Path path) throws IOException {
        return Files.readAllBytes(requireFile(path));
    }

    /**
     * 写入字节数组。
     *
     * @param path 文件路径
     * @param bytes 字节数组
     * @return 文件路径
     * @throws IOException 写入失败时抛出
     */
    public static Path writeBytes(Path path, byte[] bytes) throws IOException {
        ensureParentDir(path);
        return Files.write(requireNonNullPath(path), Objects.requireNonNull(bytes, "字节数组不能为空"), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
    }

    /**
     * 追加字节数组。
     *
     * @param path 文件路径
     * @param bytes 字节数组
     * @return 文件路径
     * @throws IOException 追加失败时抛出
     */
    public static Path appendBytes(Path path, byte[] bytes) throws IOException {
        ensureParentDir(path);
        return Files.write(requireNonNullPath(path), Objects.requireNonNull(bytes, "字节数组不能为空"), StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.WRITE);
    }

    /**
     * 打开文件输入流。
     *
     * @param path 文件路径
     * @param options 打开选项
     * @return 输入流
     * @throws IOException 打开失败时抛出
     */
    public static InputStream openInputStream(Path path, OpenOption... options) throws IOException {
        return Files.newInputStream(requireFile(path), options == null ? new OpenOption[0] : options);
    }

    /**
     * 打开文件输出流。
     *
     * @param path 文件路径
     * @param options 打开选项
     * @return 输出流
     * @throws IOException 打开失败时抛出
     */
    public static OutputStream openOutputStream(Path path, OpenOption... options) throws IOException {
        ensureParentDir(path);
        OpenOption[] openOptions = options == null || options.length == 0
                ? new OpenOption[]{StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE}
                : options;
        return Files.newOutputStream(requireNonNullPath(path), openOptions);
    }

    /**
     * 创建缓冲输入流。
     *
     * @param path 文件路径
     * @return 缓冲输入流
     * @throws IOException 创建失败时抛出
     */
    public static BufferedInputStream newBufferedInputStream(Path path) throws IOException {
        return new BufferedInputStream(openInputStream(path), BUFFER_SIZE);
    }

    /**
     * 创建缓冲输出流。
     *
     * @param path 文件路径
     * @param options 打开选项
     * @return 缓冲输出流
     * @throws IOException 创建失败时抛出
     */
    public static BufferedOutputStream newBufferedOutputStream(Path path, OpenOption... options) throws IOException {
        return new BufferedOutputStream(openOutputStream(path, options), BUFFER_SIZE);
    }

    /**
     * 复制输入流到输出流。
     *
     * @param input 输入流
     * @param output 输出流
     * @return 复制字节数
     * @throws IOException 复制失败时抛出
     */
    public static long copyStream(InputStream input, OutputStream output) throws IOException {
        Objects.requireNonNull(input, "输入流不能为空");
        Objects.requireNonNull(output, "输出流不能为空");
        return input.transferTo(output);
    }

    /**
     * 将输入流读取为字节数组。
     *
     * @param input 输入流
     * @return 字节数组
     * @throws IOException 读取失败时抛出
     */
    public static byte[] toByteArray(InputStream input) throws IOException {
        Objects.requireNonNull(input, "输入流不能为空");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        copyStream(input, output);
        return output.toByteArray();
    }

    /**
     * 将输入流写入文件。
     *
     * @param input 输入流
     * @param target 目标文件
     * @param overwrite 是否覆盖
     * @return 目标文件
     * @throws IOException 写入失败时抛出
     */
    public static Path writeFromStream(InputStream input, Path target, boolean overwrite) throws IOException {
        ensureParentDir(target);
        CopyOption[] options = overwrite ? new CopyOption[]{StandardCopyOption.REPLACE_EXISTING} : new CopyOption[0];
        Files.copy(Objects.requireNonNull(input, "输入流不能为空"), requireNonNullPath(target), options);
        return target;
    }

    /**
     * 将文件读取到输出流。
     *
     * @param source 源文件
     * @param output 输出流
     * @return 写出字节数
     * @throws IOException 读取失败时抛出
     */
    public static long readToStream(Path source, OutputStream output) throws IOException {
        try (InputStream input = openInputStream(source)) {
            return copyStream(input, output);
        }
    }

    /**
     * 文件传输到目标文件。
     *
     * @param source 源文件
     * @param target 目标文件
     * @return 传输字节数
     * @throws IOException 传输失败时抛出
     */
    public static long transferTo(Path source, Path target) throws IOException {
        ensureParentDir(target);
        try (FileChannel input = FileChannel.open(requireFile(source), StandardOpenOption.READ);
             FileChannel output = FileChannel.open(requireNonNullPath(target), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            return input.transferTo(0, input.size(), output);
        }
    }

    /**
     * 获取目录下普通文件列表。
     *
     * @param dir 目录路径
     * @return 文件列表
     * @throws IOException 读取失败时抛出
     */
    public static List<Path> listFiles(Path dir) throws IOException {
        checkIsDir(dir);
        try (Stream<Path> stream = Files.list(dir)) {
            return stream.filter(Files::isRegularFile).toList();
        }
    }

    /**
     * 获取目录下子目录列表。
     *
     * @param dir 目录路径
     * @return 子目录列表
     * @throws IOException 读取失败时抛出
     */
    public static List<Path> listDirs(Path dir) throws IOException {
        checkIsDir(dir);
        try (Stream<Path> stream = Files.list(dir)) {
            return stream.filter(Files::isDirectory).toList();
        }
    }

    /**
     * 获取目录下全部文件和目录。
     *
     * @param dir 目录路径
     * @return 路径列表
     * @throws IOException 读取失败时抛出
     */
    public static List<Path> listAll(Path dir) throws IOException {
        checkIsDir(dir);
        try (Stream<Path> stream = Files.list(dir)) {
            return stream.toList();
        }
    }

    /**
     * 递归遍历目录。
     *
     * @param dir 目录路径
     * @return 路径列表
     * @throws IOException 遍历失败时抛出
     */
    public static List<Path> walk(Path dir) throws IOException {
        checkIsDir(dir);
        try (Stream<Path> stream = Files.walk(dir)) {
            return stream.toList();
        }
    }

    /**
     * 递归遍历普通文件。
     *
     * @param dir 目录路径
     * @return 文件列表
     * @throws IOException 遍历失败时抛出
     */
    public static List<Path> walkFiles(Path dir) throws IOException {
        return findFiles(dir, path -> true);
    }

    /**
     * 递归遍历目录。
     *
     * @param dir 目录路径
     * @return 目录列表
     * @throws IOException 遍历失败时抛出
     */
    public static List<Path> walkDirs(Path dir) throws IOException {
        return findDirs(dir, path -> true);
    }

    /**
     * 按条件递归查找文件。
     *
     * @param dir 目录路径
     * @param predicate 过滤条件
     * @return 文件列表
     * @throws IOException 查找失败时抛出
     */
    public static List<Path> findFiles(Path dir, Predicate<Path> predicate) throws IOException {
        checkIsDir(dir);
        Predicate<Path> filter = predicate == null ? path -> true : predicate;
        try (Stream<Path> stream = Files.walk(dir)) {
            return stream.filter(Files::isRegularFile).filter(filter).toList();
        }
    }

    /**
     * 按条件递归查找目录。
     *
     * @param dir 目录路径
     * @param predicate 过滤条件
     * @return 目录列表
     * @throws IOException 查找失败时抛出
     */
    public static List<Path> findDirs(Path dir, Predicate<Path> predicate) throws IOException {
        checkIsDir(dir);
        Predicate<Path> filter = predicate == null ? path -> true : predicate;
        try (Stream<Path> stream = Files.walk(dir)) {
            return stream.filter(Files::isDirectory).filter(filter).toList();
        }
    }

    /**
     * 按名称查找文件或目录。
     *
     * @param dir 目录路径
     * @param name 名称
     * @return 匹配路径列表
     * @throws IOException 查找失败时抛出
     */
    public static List<Path> findByName(Path dir, String name) throws IOException {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("名称不能为空");
        }
        try (Stream<Path> stream = Files.walk(requireDir(dir))) {
            return stream.filter(path -> name.equals(getFileName(path))).toList();
        }
    }

    /**
     * 按扩展名查找文件。
     *
     * @param dir 目录路径
     * @param extNames 扩展名集合
     * @return 文件列表
     * @throws IOException 查找失败时抛出
     */
    public static List<Path> findByExtName(Path dir, Collection<String> extNames) throws IOException {
        Set<String> normalized = normalizeExtNames(extNames);
        return findFiles(dir, path -> normalized.contains(getExtName(path)));
    }

    /**
     * 统计目录下普通文件数量。
     *
     * @param dir 目录路径
     * @return 文件数量
     * @throws IOException 统计失败时抛出
     */
    public static long countFiles(Path dir) throws IOException {
        return walkFiles(dir).size();
    }

    /**
     * 统计目录数量。
     *
     * @param dir 目录路径
     * @return 目录数量
     * @throws IOException 统计失败时抛出
     */
    public static long countDirs(Path dir) throws IOException {
        return walkDirs(dir).size();
    }

    /**
     * 按条件过滤文件。
     *
     * @param files 文件集合
     * @param predicate 过滤条件
     * @return 过滤后的文件列表
     */
    public static List<Path> filterFiles(Collection<Path> files, Predicate<Path> predicate) {
        if (files == null || files.isEmpty()) {
            return List.of();
        }
        Predicate<Path> filter = predicate == null ? path -> true : predicate;
        return files.stream().filter(Objects::nonNull).filter(Files::isRegularFile).filter(filter).toList();
    }

    /**
     * 遍历目录下文件并执行处理器。
     *
     * @param dir 目录路径
     * @param consumer 文件处理器
     * @throws IOException 遍历失败时抛出
     */
    public static void forEachFile(Path dir, Consumer<Path> consumer) throws IOException {
        Objects.requireNonNull(consumer, "文件处理器不能为空");
        for (Path file : walkFiles(dir)) {
            consumer.accept(file);
        }
    }

    /**
     * 获取文件大小。
     *
     * @param path 文件路径
     * @return 文件大小，单位字节
     * @throws IOException 读取失败时抛出
     */
    public static long size(Path path) throws IOException {
        return Files.size(requireNonNullPath(path));
    }

    /**
     * 获取文件或目录大小。
     *
     * @param path 文件或目录路径
     * @return 大小，单位字节
     * @throws IOException 统计失败时抛出
     */
    public static long sizeOf(Path path) throws IOException {
        requireNonNullPath(path);
        return Files.isDirectory(path) ? sizeOfDir(path) : sizeOfFile(path);
    }

    /**
     * 获取单个文件大小。
     *
     * @param file 文件路径
     * @return 文件大小，单位字节
     * @throws IOException 读取失败时抛出
     */
    public static long sizeOfFile(Path file) throws IOException {
        return Files.size(requireFile(file));
    }

    /**
     * 获取目录总大小。
     *
     * @param dir 目录路径
     * @return 目录大小，单位字节
     * @throws IOException 统计失败时抛出
     */
    public static long sizeOfDir(Path dir) throws IOException {
        checkIsDir(dir);
        try (Stream<Path> stream = Files.walk(dir)) {
            long total = 0;
            for (Path path : stream.filter(Files::isRegularFile).toList()) {
                total += Files.size(path);
            }
            return total;
        }
    }

    /**
     * 格式化文件大小。
     *
     * @param bytes 字节数
     * @return 可读大小字符串
     */
    public static String readableSize(long bytes) {
        if (bytes < 0) {
            throw new IllegalArgumentException("字节数不能小于 0");
        }
        String[] units = {"B", "KB", "MB", "GB", "TB", "PB"};
        double value = bytes;
        int unitIndex = 0;
        while (value >= 1024 && unitIndex < units.length - 1) {
            value /= 1024;
            unitIndex++;
        }
        return unitIndex == 0 ? bytes + " B" : String.format(Locale.ROOT, "%.2f %s", value, units[unitIndex]);
    }

    /**
     * 获取最后修改时间。
     *
     * @param path 文件路径
     * @return 最后修改时间
     * @throws IOException 读取失败时抛出
     */
    public static Instant lastModifiedTime(Path path) throws IOException {
        return Files.getLastModifiedTime(requireNonNullPath(path)).toInstant();
    }

    /**
     * 获取创建时间。
     *
     * @param path 文件路径
     * @return 创建时间
     * @throws IOException 读取失败时抛出
     */
    public static Instant creationTime(Path path) throws IOException {
        return Files.readAttributes(requireNonNullPath(path), BasicFileAttributes.class).creationTime().toInstant();
    }

    /**
     * 获取最后访问时间。
     *
     * @param path 文件路径
     * @return 最后访问时间
     * @throws IOException 读取失败时抛出
     */
    public static Instant lastAccessTime(Path path) throws IOException {
        return Files.readAttributes(requireNonNullPath(path), BasicFileAttributes.class).lastAccessTime().toInstant();
    }

    /**
     * 设置最后修改时间。
     *
     * @param path 文件路径
     * @param instant 时间
     * @return 文件路径
     * @throws IOException 设置失败时抛出
     */
    public static Path setLastModifiedTime(Path path, Instant instant) throws IOException {
        Files.setLastModifiedTime(requireNonNullPath(path), FileTime.from(Objects.requireNonNull(instant, "时间不能为空")));
        return path;
    }

    /**
     * 判断文件是否已过期。
     *
     * @param path 文件路径
     * @param maxAge 最大保留时长
     * @return 已过期返回 true
     * @throws IOException 判断失败时抛出
     */
    public static boolean isExpired(Path path, Duration maxAge) throws IOException {
        if (maxAge == null || maxAge.isNegative()) {
            throw new IllegalArgumentException("最大保留时长不能为空且不能为负数");
        }
        return lastModifiedTime(path).plus(maxAge).isBefore(Instant.now());
    }

    /**
     * 判断文件是否在指定时间后修改。
     *
     * @param path 文件路径
     * @param instant 比较时间
     * @return 在指定时间后修改返回 true
     * @throws IOException 判断失败时抛出
     */
    public static boolean isModifiedAfter(Path path, Instant instant) throws IOException {
        return lastModifiedTime(path).isAfter(Objects.requireNonNull(instant, "比较时间不能为空"));
    }

    /**
     * 判断文件是否在指定时间前修改。
     *
     * @param path 文件路径
     * @param instant 比较时间
     * @return 在指定时间前修改返回 true
     * @throws IOException 判断失败时抛出
     */
    public static boolean isModifiedBefore(Path path, Instant instant) throws IOException {
        return lastModifiedTime(path).isBefore(Objects.requireNonNull(instant, "比较时间不能为空"));
    }

    /**
     * 清理非法文件名字符。
     *
     * @param fileName 文件名
     * @return 清理后的文件名
     */
    public static String cleanFileName(String fileName) {
        return sanitizeFileName(fileName);
    }

    /**
     * 安全化文件名。
     *
     * @param fileName 文件名
     * @return 安全文件名
     */
    public static String sanitizeFileName(String fileName) {
        if (fileName == null) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        return fileName.trim().replaceAll(ILLEGAL_FILE_NAME_PATTERN, "_").replaceAll("_+", "_");
    }

    /**
     * 标准化文件名。
     *
     * @param fileName 文件名
     * @return 标准化后的文件名
     */
    public static String normalizeFileName(String fileName) {
        return sanitizeFileName(fileName).replaceAll("\\s+", "_");
    }

    /**
     * 获取安全文件名，清理后为空时返回默认名称。
     *
     * @param fileName 文件名
     * @param defaultName 默认名称
     * @return 安全文件名
     */
    public static String getSafeFileName(String fileName, String defaultName) {
        String safe = sanitizeFileName(fileName);
        if (safe.isBlank() || safe.equals(".") || safe.equals("..")) {
            return sanitizeFileName(defaultName == null || defaultName.isBlank() ? "file" : defaultName);
        }
        return safe;
    }

    /**
     * 生成文件名。
     *
     * @param baseName 基础名称
     * @param extName 扩展名
     * @return 文件名
     */
    public static String generateFileName(String baseName, String extName) {
        String safeBase = getSafeFileName(baseName, "file");
        String ext = normalizeExtName(extName);
        return ext.isEmpty() ? safeBase : safeBase + "." + ext;
    }

    /**
     * 在指定目录生成不冲突文件名。
     *
     * @param dir 目录路径
     * @param originalName 原始文件名
     * @return 唯一文件路径
     * @throws IOException 目录创建失败时抛出
     */
    public static Path generateUniqueFileName(Path dir, String originalName) throws IOException {
        ensureDir(dir);
        String safeName = getSafeFileName(originalName, "file");
        String base = getBaseName(Paths.get(safeName));
        String ext = getExtName(Paths.get(safeName));
        Path candidate = dir.resolve(safeName);
        int index = 1;
        while (Files.exists(candidate)) {
            String name = base + "(" + index++ + ")" + (ext.isEmpty() ? "" : "." + ext);
            candidate = dir.resolve(name);
        }
        return candidate;
    }

    /**
     * 生成时间戳文件名。
     *
     * @param extName 扩展名
     * @return 时间戳文件名
     */
    public static String generateTimestampFileName(String extName) {
        String name = LocalDateTime.now().format(TIMESTAMP_FORMATTER) + "-" + UUID.randomUUID().toString().substring(0, 8);
        String ext = normalizeExtName(extName);
        return ext.isEmpty() ? name : name + "." + ext;
    }

    /**
     * 给文件名添加前缀。
     *
     * @param fileName 文件名
     * @param prefix 前缀
     * @return 新文件名
     */
    public static String addPrefix(String fileName, String prefix) {
        return Objects.requireNonNullElse(prefix, "") + getSafeFileName(fileName, "file");
    }

    /**
     * 给文件名添加后缀。
     *
     * @param fileName 文件名
     * @param suffix 后缀
     * @return 新文件名
     */
    public static String addSuffix(String fileName, String suffix) {
        String safe = getSafeFileName(fileName, "file");
        String base = getBaseName(Paths.get(safe));
        String ext = getExtName(Paths.get(safe));
        String newName = base + Objects.requireNonNullElse(suffix, "");
        return ext.isEmpty() ? newName : newName + "." + ext;
    }

    /**
     * 替换路径中的文件名。
     *
     * @param path 文件路径
     * @param newFileName 新文件名
     * @return 替换后的路径
     */
    public static Path replaceFileName(Path path, String newFileName) {
        requireNonNullPath(path);
        checkFileNameSafe(newFileName);
        Path parent = path.getParent();
        return parent == null ? Paths.get(newFileName) : parent.resolve(newFileName);
    }

    /**
     * 截断超长文件名。
     *
     * @param fileName 文件名
     * @param maxLength 最大长度
     * @return 截断后的文件名
     */
    public static String truncateFileName(String fileName, int maxLength) {
        if (maxLength <= 0) {
            throw new IllegalArgumentException("最大长度必须大于 0");
        }
        String safe = getSafeFileName(fileName, "file");
        if (safe.length() <= maxLength) {
            return safe;
        }
        String ext = getExtName(Paths.get(safe));
        String base = getBaseName(Paths.get(safe));
        if (ext.isEmpty() || ext.length() + 1 >= maxLength) {
            return safe.substring(0, maxLength);
        }
        int baseMax = maxLength - ext.length() - 1;
        return base.substring(0, Math.min(base.length(), baseMax)) + "." + ext;
    }

    /**
     * 获取 MIME 类型，无法探测时按扩展名兜底。
     *
     * @param path 文件路径
     * @return MIME 类型，不存在时返回 application/octet-stream
     * @throws IOException 探测失败时抛出
     */
    public static String getMimeType(Path path) throws IOException {
        String type = probeContentType(path);
        if (type != null && !type.isBlank()) {
            return type;
        }
        return MIME_FALLBACKS.getOrDefault(getExtName(path), "application/octet-stream");
    }

    /**
     * 探测文件内容类型。
     *
     * @param path 文件路径
     * @return MIME 类型，无法探测时返回 null
     * @throws IOException 探测失败时抛出
     */
    public static String probeContentType(Path path) throws IOException {
        return Files.probeContentType(requireNonNullPath(path));
    }

    /**
     * 判断文件扩展名是否匹配。
     *
     * @param path 文件路径
     * @param extName 扩展名
     * @return 匹配返回 true
     */
    public static boolean isExtName(Path path, String extName) {
        return getExtName(path).equals(normalizeExtName(extName));
    }

    /**
     * 判断文件是否存在扩展名。
     *
     * @param path 文件路径
     * @return 存在扩展名返回 true
     */
    public static boolean hasExtName(Path path) {
        return !getExtName(path).isEmpty();
    }

    /**
     * 判断是否为图片文件。
     *
     * @param path 文件路径
     * @return 是图片返回 true
     */
    public static boolean isImage(Path path) {
        return Set.of("jpg", "jpeg", "png", "gif", "bmp", "webp", "svg").contains(getExtName(path));
    }

    /**
     * 判断是否为视频文件。
     *
     * @param path 文件路径
     * @return 是视频返回 true
     */
    public static boolean isVideo(Path path) {
        return Set.of("mp4", "mov", "avi", "mkv", "wmv", "flv", "webm").contains(getExtName(path));
    }

    /**
     * 判断是否为音频文件。
     *
     * @param path 文件路径
     * @return 是音频返回 true
     */
    public static boolean isAudio(Path path) {
        return Set.of("mp3", "wav", "aac", "flac", "ogg", "m4a").contains(getExtName(path));
    }

    /**
     * 判断是否为文本文件。
     *
     * @param path 文件路径
     * @return 是文本返回 true
     */
    public static boolean isText(Path path) {
        return Set.of("txt", "csv", "json", "xml", "yml", "yaml", "properties", "md", "log", "sql", "java", "js", "ts", "html", "css").contains(getExtName(path));
    }

    /**
     * 判断是否为 PDF 文件。
     *
     * @param path 文件路径
     * @return 是 PDF 返回 true
     */
    public static boolean isPdf(Path path) {
        return isExtName(path, "pdf");
    }

    /**
     * 判断是否为 Excel 文件。
     *
     * @param path 文件路径
     * @return 是 Excel 返回 true
     */
    public static boolean isExcel(Path path) {
        return Set.of("xls", "xlsx", "xlsm", "csv").contains(getExtName(path));
    }

    /**
     * 判断是否为 Word 文件。
     *
     * @param path 文件路径
     * @return 是 Word 返回 true
     */
    public static boolean isWord(Path path) {
        return Set.of("doc", "docx").contains(getExtName(path));
    }

    /**
     * 判断是否为压缩文件。
     *
     * @param path 文件路径
     * @return 是压缩文件返回 true
     */
    public static boolean isArchive(Path path) {
        return Set.of("zip", "gz", "gzip", "jar", "war", "tar", "7z", "rar").contains(getExtName(path));
    }

    /**
     * 判断文件扩展名是否在允许范围内。
     *
     * @param path 文件路径
     * @param extNames 允许的扩展名集合
     * @return 匹配返回 true
     */
    public static boolean matchExtName(Path path, Collection<String> extNames) {
        return normalizeExtNames(extNames).contains(getExtName(path));
    }

    /**
     * 检查目标路径是否安全。
     *
     * @param baseDir 基础目录
     * @param target 目标路径
     * @return 目标路径
     */
    public static Path checkPathSafe(Path baseDir, Path target) {
        return requireSafePath(baseDir, target);
    }

    /**
     * 检查文件名是否安全。
     *
     * @param fileName 文件名
     * @return 文件名
     */
    public static String checkFileNameSafe(String fileName) {
        if (!isValidFileName(fileName)) {
            throw new IllegalArgumentException("非法文件名：" + fileName);
        }
        return fileName;
    }

    /**
     * 检查扩展名是否在白名单内。
     *
     * @param path 文件路径
     * @param allowedExtNames 允许的扩展名集合
     * @return 文件路径
     */
    public static Path checkExtNameAllowed(Path path, Collection<String> allowedExtNames) {
        if (!matchExtName(path, allowedExtNames)) {
            throw new IllegalArgumentException("文件扩展名不允许：" + getExtName(path));
        }
        return path;
    }

    /**
     * 检查文件大小是否不超过限制。
     *
     * @param path 文件路径
     * @param maxBytes 最大字节数
     * @return 文件路径
     * @throws IOException 读取大小失败时抛出
     */
    public static Path checkSizeLimit(Path path, long maxBytes) throws IOException {
        if (maxBytes < 0) {
            throw new IllegalArgumentException("最大字节数不能小于 0");
        }
        if (size(path) > maxBytes) {
            throw new IllegalArgumentException("文件大小超过限制：" + maxBytes + " bytes");
        }
        return path;
    }

    /**
     * 检查文件是否可读。
     *
     * @param path 文件路径
     * @return 文件路径
     */
    public static Path checkReadable(Path path) {
        requireNonNullPath(path);
        if (!Files.isReadable(path)) {
            throw new IllegalArgumentException("文件不可读：" + path);
        }
        return path;
    }

    /**
     * 检查文件是否可写。
     *
     * @param path 文件路径
     * @return 文件路径
     */
    public static Path checkWritable(Path path) {
        requireNonNullPath(path);
        if (!Files.isWritable(path)) {
            throw new IllegalArgumentException("文件不可写：" + path);
        }
        return path;
    }

    /**
     * 检查路径是否存在。
     *
     * @param path 文件路径
     * @return 文件路径
     */
    public static Path checkExists(Path path) {
        requireNonNullPath(path);
        if (Files.notExists(path)) {
            throw new IllegalArgumentException("路径不存在：" + path);
        }
        return path;
    }

    /**
     * 检查路径是否不存在。
     *
     * @param path 文件路径
     * @return 文件路径
     */
    public static Path checkNotExists(Path path) {
        requireNonNullPath(path);
        if (Files.exists(path)) {
            throw new IllegalArgumentException("路径已存在：" + path);
        }
        return path;
    }

    /**
     * 检查路径是否为普通文件。
     *
     * @param path 文件路径
     * @return 文件路径
     */
    public static Path checkIsFile(Path path) {
        requireNonNullPath(path);
        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException("路径不是普通文件：" + path);
        }
        return path;
    }

    /**
     * 检查路径是否为目录。
     *
     * @param path 目录路径
     * @return 目录路径
     */
    public static Path checkIsDir(Path path) {
        requireNonNullPath(path);
        if (!Files.isDirectory(path)) {
            throw new IllegalArgumentException("路径不是目录：" + path);
        }
        return path;
    }

    /**
     * 防止目录穿越并返回安全路径。
     *
     * @param baseDir 基础目录
     * @param target 目标路径
     * @return 安全目标路径
     */
    public static Path preventPathTraversal(Path baseDir, Path target) {
        return requireSafePath(baseDir, target);
    }

    /**
     * 在基础目录下解析安全路径。
     *
     * @param baseDir 基础目录
     * @param pathSegments 路径片段
     * @return 安全路径
     */
    public static Path resolveSafePath(Path baseDir, String... pathSegments) {
        requireNonNullPath(baseDir);
        if (pathSegments == null || pathSegments.length == 0) {
            return baseDir.toAbsolutePath().normalize();
        }
        Path target = baseDir;
        for (String segment : pathSegments) {
            if (segment == null || segment.isBlank()) {
                throw new IllegalArgumentException("路径片段不能为空");
            }
            target = target.resolve(segment);
        }
        return requireSafePath(baseDir, target.toAbsolutePath().normalize());
    }

    /**
     * 设置可读权限。
     *
     * @param path 文件路径
     * @param readable 是否可读
     * @return 设置成功返回 true
     */
    public static boolean setReadable(Path path, boolean readable) {
        return requireNonNullPath(path).toFile().setReadable(readable);
    }

    /**
     * 设置可写权限。
     *
     * @param path 文件路径
     * @param writable 是否可写
     * @return 设置成功返回 true
     */
    public static boolean setWritable(Path path, boolean writable) {
        return requireNonNullPath(path).toFile().setWritable(writable);
    }

    /**
     * 设置可执行权限。
     *
     * @param path 文件路径
     * @param executable 是否可执行
     * @return 设置成功返回 true
     */
    public static boolean setExecutable(Path path, boolean executable) {
        return requireNonNullPath(path).toFile().setExecutable(executable);
    }

    /**
     * 设置只读权限。
     *
     * @param path 文件路径
     * @return 设置成功返回 true
     */
    public static boolean setReadOnly(Path path) {
        return requireNonNullPath(path).toFile().setReadOnly();
    }

    /**
     * 获取文件所有者。
     *
     * @param path 文件路径
     * @return 文件所有者
     * @throws IOException 读取失败时抛出
     */
    public static UserPrincipal getOwner(Path path) throws IOException {
        return Files.getOwner(requireNonNullPath(path));
    }

    /**
     * 设置文件所有者。
     *
     * @param path 文件路径
     * @param ownerName 所有者名称
     * @return 文件路径
     * @throws IOException 设置失败时抛出
     */
    public static Path setOwner(Path path, String ownerName) throws IOException {
        if (ownerName == null || ownerName.isBlank()) {
            throw new IllegalArgumentException("所有者名称不能为空");
        }
        UserPrincipalLookupService service = FileSystems.getDefault().getUserPrincipalLookupService();
        Files.setOwner(requireNonNullPath(path), service.lookupPrincipalByName(ownerName));
        return path;
    }

    /**
     * 获取 POSIX 文件权限。
     *
     * @param path 文件路径
     * @return 权限集合
     * @throws IOException 读取失败时抛出
     */
    public static Set<PosixFilePermission> getPermissions(Path path) throws IOException {
        ensurePosixSupported(path);
        return Files.getPosixFilePermissions(requireNonNullPath(path));
    }

    /**
     * 设置 POSIX 文件权限。
     *
     * @param path 文件路径
     * @param permissions 权限集合
     * @return 文件路径
     * @throws IOException 设置失败时抛出
     */
    public static Path setPermissions(Path path, Set<PosixFilePermission> permissions) throws IOException {
        ensurePosixSupported(path);
        Files.setPosixFilePermissions(requireNonNullPath(path), Objects.requireNonNull(permissions, "权限集合不能为空"));
        return path;
    }

    /**
     * 获取基础文件属性。
     *
     * @param path 文件路径
     * @return 文件属性
     * @throws IOException 读取失败时抛出
     */
    public static BasicFileAttributes getAttributes(Path path) throws IOException {
        return Files.readAttributes(requireNonNullPath(path), BasicFileAttributes.class);
    }

    /**
     * 设置文件属性。
     *
     * @param path 文件路径
     * @param attribute 属性名称
     * @param value 属性值
     * @return 文件路径
     * @throws IOException 设置失败时抛出
     */
    public static Path setAttribute(Path path, String attribute, Object value) throws IOException {
        if (attribute == null || attribute.isBlank()) {
            throw new IllegalArgumentException("属性名称不能为空");
        }
        Files.setAttribute(requireNonNullPath(path), attribute, value);
        return path;
    }

    /**
     * 判断文件是否只读。
     *
     * @param path 文件路径
     * @return 只读返回 true
     */
    public static boolean isReadonly(Path path) {
        return !isWritable(path);
    }

    /**
     * 计算文件 MD5。
     *
     * @param path 文件路径
     * @return MD5 十六进制字符串
     * @throws IOException 读取失败时抛出
     */
    public static String md5(Path path) throws IOException {
        return hash(path, "MD5");
    }

    /**
     * 计算文件 SHA-1。
     *
     * @param path 文件路径
     * @return SHA-1 十六进制字符串
     * @throws IOException 读取失败时抛出
     */
    public static String sha1(Path path) throws IOException {
        return hash(path, "SHA-1");
    }

    /**
     * 计算文件 SHA-256。
     *
     * @param path 文件路径
     * @return SHA-256 十六进制字符串
     * @throws IOException 读取失败时抛出
     */
    public static String sha256(Path path) throws IOException {
        return hash(path, "SHA-256");
    }

    /**
     * 按指定算法计算文件哈希。
     *
     * @param path 文件路径
     * @param algorithm 摘要算法
     * @return 十六进制哈希字符串
     * @throws IOException 读取失败时抛出
     */
    public static String hash(Path path, String algorithm) throws IOException {
        if (algorithm == null || algorithm.isBlank()) {
            throw new IllegalArgumentException("摘要算法不能为空");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            try (InputStream input = newBufferedInputStream(requireFile(path));
                 DigestInputStream digestInput = new DigestInputStream(input, digest)) {
                byte[] buffer = new byte[BUFFER_SIZE];
                while (digestInput.read(buffer) != -1) {
                    // 读取即可触发摘要计算
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalArgumentException("不支持的摘要算法：" + algorithm, ex);
        }
    }

    /**
     * 校验文件 MD5。
     *
     * @param path 文件路径
     * @param expected 期望 MD5
     * @return 匹配返回 true
     * @throws IOException 读取失败时抛出
     */
    public static boolean verifyMd5(Path path, String expected) throws IOException {
        return md5(path).equalsIgnoreCase(requireHash(expected));
    }

    /**
     * 校验文件 SHA-256。
     *
     * @param path 文件路径
     * @param expected 期望 SHA-256
     * @return 匹配返回 true
     * @throws IOException 读取失败时抛出
     */
    public static boolean verifySha256(Path path, String expected) throws IOException {
        return sha256(path).equalsIgnoreCase(requireHash(expected));
    }

    /**
     * 判断两个文件内容是否一致。
     *
     * @param first 第一个文件
     * @param second 第二个文件
     * @return 内容一致返回 true
     * @throws IOException 比较失败时抛出
     */
    public static boolean sameContent(Path first, Path second) throws IOException {
        return Files.mismatch(requireFile(first), requireFile(second)) == -1L;
    }

    /**
     * 比较两个文件内容。
     *
     * @param first 第一个文件
     * @param second 第二个文件
     * @return 内容相同返回 0，否则按首个不同字节返回比较结果
     * @throws IOException 比较失败时抛出
     */
    public static int compareContent(Path first, Path second) throws IOException {
        try (InputStream left = openInputStream(first); InputStream right = openInputStream(second)) {
            int a;
            int b;
            do {
                a = left.read();
                b = right.read();
                if (a != b) {
                    return Integer.compare(a, b);
                }
            } while (a != -1);
            return 0;
        }
    }

    /**
     * 计算 CRC32 校验值。
     *
     * @param path 文件路径
     * @return CRC32 值
     * @throws IOException 读取失败时抛出
     */
    public static long checksumCRC32(Path path) throws IOException {
        return checksum(path, new CRC32());
    }

    /**
     * 计算 Adler32 校验值。
     *
     * @param path 文件路径
     * @return Adler32 值
     * @throws IOException 读取失败时抛出
     */
    public static long checksumAdler32(Path path) throws IOException {
        return checksum(path, new Adler32());
    }

    /**
     * 获取独占文件锁。
     *
     * @param path 锁文件路径
     * @return 文件锁句柄
     * @throws IOException 加锁失败时抛出
     */
    public static FileLockHandle lock(Path path) throws IOException {
        ensureParentDir(path);
        FileChannel channel = FileChannel.open(requireNonNullPath(path), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        try {
            return new FileLockHandle(channel, channel.lock());
        } catch (IOException | RuntimeException ex) {
            channel.close();
            throw ex;
        }
    }

    /**
     * 尝试获取独占文件锁。
     *
     * @param path 锁文件路径
     * @return 获取成功返回锁句柄，否则返回 Optional.empty
     * @throws IOException 加锁失败时抛出
     */
    public static Optional<FileLockHandle> tryLock(Path path) throws IOException {
        ensureParentDir(path);
        FileChannel channel = FileChannel.open(requireNonNullPath(path), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        try {
            FileLock fileLock = channel.tryLock();
            if (fileLock == null) {
                channel.close();
                return Optional.empty();
            }
            return Optional.of(new FileLockHandle(channel, fileLock));
        } catch (OverlappingFileLockException ex) {
            channel.close();
            return Optional.empty();
        } catch (IOException | RuntimeException ex) {
            channel.close();
            throw ex;
        }
    }

    /**
     * 判断文件当前是否被锁定。
     *
     * @param path 锁文件路径
     * @return 已锁定返回 true
     * @throws IOException 判断失败时抛出
     */
    public static boolean isLocked(Path path) throws IOException {
        Optional<FileLockHandle> handle = tryLock(path);
        if (handle.isPresent()) {
            handle.get().close();
            return false;
        }
        return true;
    }

    /**
     * 在文件锁保护下执行任务。
     *
     * @param path 锁文件路径
     * @param callable 任务
     * @param <T> 返回类型
     * @return 任务返回值
     * @throws Exception 任务或加锁失败时抛出
     */
    public static <T> T withLock(Path path, Callable<T> callable) throws Exception {
        Objects.requireNonNull(callable, "任务不能为空");
        try (FileLockHandle ignored = lock(path)) {
            return callable.call();
        }
    }

    /**
     * 在文件锁保护下写入字节数组。
     *
     * @param path 文件路径
     * @param bytes 字节数组
     * @return 文件路径
     * @throws IOException 写入失败时抛出
     */
    public static Path writeWithLock(Path path, byte[] bytes) throws IOException {
        try {
            return withLock(path, () -> writeBytes(path, bytes));
        } catch (IOException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IOException("加锁写入失败", ex);
        }
    }

    /**
     * 在文件锁保护下读取字节数组。
     *
     * @param path 文件路径
     * @return 字节数组
     * @throws IOException 读取失败时抛出
     */
    public static byte[] readWithLock(Path path) throws IOException {
        try {
            return withLock(path, () -> readBytes(path));
        } catch (IOException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IOException("加锁读取失败", ex);
        }
    }

    /**
     * 获取系统临时目录。
     *
     * @return 系统临时目录
     */
    public static Path getTempDir() {
        return Paths.get(System.getProperty("java.io.tmpdir")).toAbsolutePath().normalize();
    }

    /**
     * 写入临时文件。
     *
     * @param prefix 文件名前缀
     * @param suffix 文件名后缀
     * @param bytes 字节数组
     * @return 临时文件路径
     * @throws IOException 写入失败时抛出
     */
    public static Path writeTempFile(String prefix, String suffix, byte[] bytes) throws IOException {
        Path temp = createTempFile(prefix, suffix);
        return writeBytes(temp, bytes);
    }

    /**
     * 删除临时文件。
     *
     * @param tempFile 临时文件路径
     * @return 删除成功返回 true
     * @throws IOException 删除失败时抛出
     */
    public static boolean deleteTempFile(Path tempFile) throws IOException {
        return deleteIfExists(tempFile);
    }

    /**
     * 清理系统临时目录中的直接子项。
     *
     * @throws IOException 清理失败时抛出
     */
    public static void cleanTempDir() throws IOException {
        cleanDir(getTempDir());
    }

    /**
     * 清理指定目录中过期的普通文件。
     *
     * @param dir 目录路径
     * @param maxAge 最大保留时长
     * @return 删除的文件数量
     * @throws IOException 清理失败时抛出
     */
    public static long cleanExpiredTempFiles(Path dir, Duration maxAge) throws IOException {
        checkIsDir(dir);
        if (maxAge == null || maxAge.isNegative()) {
            throw new IllegalArgumentException("最大保留时长不能为空且不能为负数");
        }
        Instant cutoff = Instant.now().minus(maxAge);
        long count = 0;
        for (Path file : walkFiles(dir)) {
            if (lastModifiedTime(file).isBefore(cutoff) && Files.deleteIfExists(file)) {
                count++;
            }
        }
        return count;
    }

    /**
     * 将临时文件移动到目标路径。
     *
     * @param tempFile 临时文件
     * @param target 目标文件
     * @return 目标文件
     * @throws IOException 移动失败时抛出
     */
    public static Path moveTempToTarget(Path tempFile, Path target) throws IOException {
        return moveFile(tempFile, target);
    }

    /**
     * 在缓存目录创建文件。
     *
     * @param cacheDir 缓存目录
     * @param fileName 文件名
     * @return 缓存文件路径
     * @throws IOException 创建失败时抛出
     */
    public static Path createCacheFile(Path cacheDir, String fileName) throws IOException {
        ensureDir(cacheDir);
        Path file = cacheDir.resolve(getSafeFileName(fileName, "cache"));
        return createFileIfAbsent(file);
    }

    /**
     * 清理缓存目录。
     *
     * @param cacheDir 缓存目录
     * @throws IOException 清理失败时抛出
     */
    public static void cleanCacheDir(Path cacheDir) throws IOException {
        cleanDir(cacheDir);
    }

    /**
     * 获取上传根路径。
     *
     * @param baseDir 基础目录
     * @return 上传根路径
     * @throws IOException 创建失败时抛出
     */
    public static Path getUploadPath(Path baseDir) throws IOException {
        return ensureDir(baseDir);
    }

    /**
     * 构建上传保存路径。
     *
     * @param baseDir 基础目录
     * @param fileName 文件名
     * @return 上传文件路径
     * @throws IOException 创建目录失败时抛出
     */
    public static Path buildUploadPath(Path baseDir, String fileName) throws IOException {
        Path dir = ensureDir(baseDir.resolve(buildDateDir()));
        return generateUniqueFileName(dir, fileName);
    }

    /**
     * 保存上传输入流。
     *
     * @param input 上传输入流
     * @param target 目标路径
     * @param overwrite 是否覆盖
     * @return 目标路径
     * @throws IOException 保存失败时抛出
     */
    public static Path saveUploadFile(InputStream input, Path target, boolean overwrite) throws IOException {
        return writeFromStream(input, target, overwrite);
    }

    /**
     * 解析下载文件路径并防止目录穿越。
     *
     * @param baseDir 基础目录
     * @param pathSegments 路径片段
     * @return 下载文件路径
     */
    public static Path resolveDownloadFile(Path baseDir, String... pathSegments) {
        return resolveSafePath(baseDir, pathSegments);
    }

    /**
     * 获取下载文件名。
     *
     * @param path 文件路径
     * @return 下载文件名
     */
    public static String getDownloadFileName(Path path) {
        return getFileName(path);
    }

    /**
     * 编码下载文件名。
     *
     * @param fileName 文件名
     * @return URL 编码后的文件名
     */
    public static String encodeDownloadFileName(String fileName) {
        return URLEncoder.encode(getSafeFileName(fileName, "download"), StandardCharsets.UTF_8).replace("+", "%20");
    }

    /**
     * 检查上传文件。
     *
     * @param file 文件路径
     * @param maxBytes 最大字节数
     * @param allowedExtNames 允许的扩展名集合
     * @return 文件路径
     * @throws IOException 校验失败时抛出
     */
    public static Path checkUploadFile(Path file, long maxBytes, Collection<String> allowedExtNames) throws IOException {
        checkIsFile(file);
        checkSizeLimit(file, maxBytes);
        if (allowedExtNames != null && !allowedExtNames.isEmpty()) {
            checkExtNameAllowed(file, allowedExtNames);
        }
        return file;
    }

    /**
     * 检查下载文件。
     *
     * @param baseDir 基础目录
     * @param file 下载文件
     * @return 文件路径
     */
    public static Path checkDownloadFile(Path baseDir, Path file) {
        Path safe = requireSafePath(baseDir, file);
        return requireFile(safe);
    }

    /**
     * 构建日期分层目录。
     *
     * @return 日期目录路径
     */
    public static Path buildDateDir() {
        return Paths.get(LocalDate.now().format(DATE_DIR_FORMATTER));
    }

    /**
     * 压缩文件或目录为 ZIP。
     *
     * @param source 源路径
     * @param zipPath ZIP 文件路径
     * @return ZIP 文件路径
     * @throws IOException 压缩失败时抛出
     */
    public static Path zip(Path source, Path zipPath) throws IOException {
        requireNonNullPath(source);
        if (Files.isDirectory(source)) {
            return zipDir(source, zipPath);
        }
        return zipFile(source, zipPath);
    }

    /**
     * 压缩单个文件为 ZIP。
     *
     * @param sourceFile 源文件
     * @param zipPath ZIP 文件路径
     * @return ZIP 文件路径
     * @throws IOException 压缩失败时抛出
     */
    public static Path zipFile(Path sourceFile, Path zipPath) throws IOException {
        checkIsFile(sourceFile);
        ensureParentDir(zipPath);
        try (ZipOutputStream output = new ZipOutputStream(newBufferedOutputStream(zipPath))) {
            output.putNextEntry(new ZipEntry(getFileName(sourceFile)));
            Files.copy(sourceFile, output);
            output.closeEntry();
        }
        return zipPath;
    }

    /**
     * 压缩目录为 ZIP。
     *
     * @param sourceDir 源目录
     * @param zipPath ZIP 文件路径
     * @return ZIP 文件路径
     * @throws IOException 压缩失败时抛出
     */
    public static Path zipDir(Path sourceDir, Path zipPath) throws IOException {
        checkIsDir(sourceDir);
        ensureParentDir(zipPath);
        try (ZipOutputStream output = new ZipOutputStream(newBufferedOutputStream(zipPath));
             Stream<Path> stream = Files.walk(sourceDir)) {
            for (Path path : stream.toList()) {
                if (path.equals(sourceDir)) {
                    continue;
                }
                String entryName = sourceDir.relativize(path).toString().replace(File.separatorChar, '/');
                if (Files.isDirectory(path)) {
                    output.putNextEntry(new ZipEntry(entryName + "/"));
                    output.closeEntry();
                } else {
                    output.putNextEntry(new ZipEntry(entryName));
                    Files.copy(path, output);
                    output.closeEntry();
                }
            }
        }
        return zipPath;
    }

    /**
     * 解压 ZIP 文件。
     *
     * @param zipPath ZIP 文件路径
     * @param targetDir 目标目录
     * @return 目标目录
     * @throws IOException 解压失败时抛出
     */
    public static Path unzip(Path zipPath, Path targetDir) throws IOException {
        checkIsFile(zipPath);
        ensureDir(targetDir);
        try (ZipInputStream input = new ZipInputStream(newBufferedInputStream(zipPath))) {
            ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                Path target = preventZipSlip(targetDir, entry);
                if (entry.isDirectory()) {
                    ensureDir(target);
                } else {
                    ensureParentDir(target);
                    try (OutputStream output = newBufferedOutputStream(target)) {
                        input.transferTo(output);
                    }
                }
                input.closeEntry();
            }
        }
        return targetDir;
    }

    /**
     * 获取 ZIP 条目列表。
     *
     * @param zipPath ZIP 文件路径
     * @return 条目名称列表
     * @throws IOException 读取失败时抛出
     */
    public static List<String> listZipEntries(Path zipPath) throws IOException {
        checkIsFile(zipPath);
        List<String> entries = new ArrayList<>();
        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            zipFile.stream().forEach(entry -> entries.add(entry.getName()));
        }
        return entries;
    }

    /**
     * 解压 ZIP 中的指定条目。
     *
     * @param zipPath ZIP 文件路径
     * @param entryName 条目名称
     * @param target 目标文件
     * @return 目标文件
     * @throws IOException 解压失败时抛出
     */
    public static Path extractZipEntry(Path zipPath, String entryName, Path target) throws IOException {
        if (entryName == null || entryName.isBlank()) {
            throw new IllegalArgumentException("条目名称不能为空");
        }
        checkIsFile(zipPath);
        ensureParentDir(target);
        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            ZipEntry entry = zipFile.getEntry(entryName);
            if (entry == null || entry.isDirectory()) {
                throw new IllegalArgumentException("ZIP 条目不存在或不是文件：" + entryName);
            }
            try (InputStream input = zipFile.getInputStream(entry)) {
                writeFromStream(input, target, true);
            }
        }
        return target;
    }

    /**
     * 检查 ZIP 解压是否安全。
     *
     * @param zipPath ZIP 文件路径
     * @param targetDir 目标目录
     * @return 安全返回 true
     * @throws IOException 检查失败时抛出
     */
    public static boolean checkZipSafe(Path zipPath, Path targetDir) throws IOException {
        checkIsFile(zipPath);
        ensureDir(targetDir);
        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            for (ZipEntry entry : zipFile.stream().toList()) {
                preventZipSlip(targetDir, entry);
            }
        }
        return true;
    }

    /**
     * 防止 Zip Slip 并返回条目目标路径。
     *
     * @param targetDir 目标目录
     * @param entry ZIP 条目
     * @return 安全目标路径
     * @throws IOException 路径不安全时抛出
     */
    public static Path preventZipSlip(Path targetDir, ZipEntry entry) throws IOException {
        Objects.requireNonNull(entry, "ZIP 条目不能为空");
        Path target = ensureDir(targetDir).resolve(entry.getName()).normalize();
        Path base = targetDir.toRealPath();
        if (!target.toAbsolutePath().normalize().startsWith(base)) {
            throw new IOException("检测到不安全的 ZIP 条目：" + entry.getName());
        }
        return target;
    }

    /**
     * 将 classpath 资源复制到文件。
     *
     * @param resourceName 资源名称
     * @param target 目标文件
     * @return 目标文件
     * @throws IOException 复制失败时抛出
     */
    public static Path copyResourceToFile(String resourceName, Path target) throws IOException {
        try (InputStream input = openClasspathResource(resourceName)) {
            return writeFromStream(input, target, true);
        }
    }

    /**
     * 读取 classpath 文本资源。
     *
     * @param resourceName 资源名称
     * @param charset 字符集
     * @return 资源内容
     * @throws IOException 读取失败时抛出
     */
    public static String readClasspathFile(String resourceName, Charset charset) throws IOException {
        try (InputStream input = openClasspathResource(resourceName)) {
            return new String(toByteArray(input), requireNonNullCharset(charset));
        }
    }

    /**
     * 获取 classpath 资源路径。
     *
     * @param resourceName 资源名称
     * @return 资源路径
     */
    public static Path getClasspathPath(String resourceName) {
        try {
            URI uri = getClasspathResourceUri(resourceName);
            if (!"file".equalsIgnoreCase(uri.getScheme())) {
                throw new IllegalArgumentException("资源不是文件系统路径：" + resourceName);
            }
            return Paths.get(uri);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("资源路径非法：" + resourceName, ex);
        }
    }

    /**
     * 序列化对象到文件。
     *
     * @param path 文件路径
     * @param object 可序列化对象
     * @return 文件路径
     * @throws IOException 写入失败时抛出
     */
    public static Path writeObject(Path path, Serializable object) throws IOException {
        ensureParentDir(path);
        try (ObjectOutputStream output = new ObjectOutputStream(newBufferedOutputStream(path))) {
            output.writeObject(Objects.requireNonNull(object, "序列化对象不能为空"));
        }
        return path;
    }

    /**
     * 从文件反序列化对象。
     *
     * @param path 文件路径
     * @param type 目标类型
     * @param <T> 目标类型
     * @return 反序列化对象
     * @throws IOException 读取失败时抛出
     * @throws ClassNotFoundException 类型不存在时抛出
     */
    public static <T> T readObject(Path path, Class<T> type) throws IOException, ClassNotFoundException {
        Objects.requireNonNull(type, "目标类型不能为空");
        try (ObjectInputStream input = new ObjectInputStream(newBufferedInputStream(requireFile(path)))) {
            Object object = input.readObject();
            return type.cast(object);
        }
    }

    /**
     * 拆分大文件。
     *
     * @param source 源文件
     * @param targetDir 目标目录
     * @param partSize 每个分片大小
     * @return 分片文件列表
     * @throws IOException 拆分失败时抛出
     */
    public static List<Path> splitFile(Path source, Path targetDir, long partSize) throws IOException {
        checkIsFile(source);
        if (partSize <= 0) {
            throw new IllegalArgumentException("分片大小必须大于 0");
        }
        ensureDir(targetDir);
        List<Path> parts = new ArrayList<>();
        byte[] buffer = new byte[BUFFER_SIZE];
        try (InputStream input = newBufferedInputStream(source)) {
            int index = 1;
            int read;
            long writtenInPart = 0;
            OutputStream output = null;
            try {
                while ((read = input.read(buffer)) != -1) {
                    int offset = 0;
                    while (offset < read) {
                        if (output == null || writtenInPart >= partSize) {
                            if (output != null) {
                                output.close();
                            }
                            Path part = targetDir.resolve(getFileName(source) + ".part" + String.format(Locale.ROOT, "%03d", index++));
                            parts.add(part);
                            output = newBufferedOutputStream(part);
                            writtenInPart = 0;
                        }
                        int length = (int) Math.min(read - offset, partSize - writtenInPart);
                        output.write(buffer, offset, length);
                        offset += length;
                        writtenInPart += length;
                    }
                }
            } finally {
                if (output != null) {
                    output.close();
                }
            }
        }
        return parts;
    }

    /**
     * 合并多个文件。
     *
     * @param parts 分片文件列表
     * @param target 目标文件
     * @return 目标文件
     * @throws IOException 合并失败时抛出
     */
    public static Path mergeFiles(List<Path> parts, Path target) throws IOException {
        if (parts == null || parts.isEmpty()) {
            throw new IllegalArgumentException("分片文件列表不能为空");
        }
        ensureParentDir(target);
        try (OutputStream output = newBufferedOutputStream(target)) {
            for (Path part : parts) {
                checkIsFile(part);
                Files.copy(part, output);
            }
        }
        return target;
    }

    /**
     * 读取文件尾部指定行数。
     *
     * @param path 文件路径
     * @param lineCount 行数
     * @param charset 字符集
     * @return 尾部行列表
     * @throws IOException 读取失败时抛出
     */
    public static List<String> tail(Path path, int lineCount, Charset charset) throws IOException {
        return readLastLines(path, lineCount, charset);
    }

    /**
     * 监听文件变化事件直到超时。
     *
     * @param path 文件路径
     * @param consumer 事件处理器
     * @param timeout 超时时长
     * @throws IOException 监听失败时抛出
     * @throws InterruptedException 等待中断时抛出
     */
    public static void watch(Path path, Consumer<WatchEvent<?>> consumer, Duration timeout) throws IOException, InterruptedException {
        requireNonNullPath(path);
        Objects.requireNonNull(consumer, "事件处理器不能为空");
        Path dir = Files.isDirectory(path) ? path : path.toAbsolutePath().getParent();
        if (dir == null) {
            throw new IllegalArgumentException("无法确定监听目录：" + path);
        }
        try (WatchService service = FileSystems.getDefault().newWatchService()) {
            watchDir(dir, service);
            long millis = timeout == null ? 0L : Math.max(0L, timeout.toMillis());
            WatchKey key = millis == 0L ? service.take() : service.poll(millis, TimeUnit.MILLISECONDS);
            if (key != null) {
                for (WatchEvent<?> event : key.pollEvents()) {
                    consumer.accept(event);
                }
                key.reset();
            }
        }
    }

    /**
     * 注册目录监听。
     *
     * @param dir 目录路径
     * @param watchService 监听服务
     * @param events 监听事件类型
     * @return 监听键
     * @throws IOException 注册失败时抛出
     */
    public static WatchKey watchDir(Path dir, WatchService watchService, WatchEvent.Kind<?>... events) throws IOException {
        checkIsDir(dir);
        Objects.requireNonNull(watchService, "监听服务不能为空");
        WatchEvent.Kind<?>[] kinds = events == null || events.length == 0
                ? new WatchEvent.Kind<?>[]{java.nio.file.StandardWatchEventKinds.ENTRY_CREATE, java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY, java.nio.file.StandardWatchEventKinds.ENTRY_DELETE}
                : events;
        return dir.register(watchService, kinds);
    }

    /**
     * 等待文件生成。
     *
     * @param path 文件路径
     * @param timeout 超时时长
     * @param interval 检查间隔
     * @return 文件出现返回 true，超时返回 false
     * @throws InterruptedException 等待中断时抛出
     */
    public static boolean waitForFile(Path path, Duration timeout, Duration interval) throws InterruptedException {
        requireNonNullPath(path);
        long timeoutMillis = timeout == null ? 0L : Math.max(0L, timeout.toMillis());
        long intervalMillis = interval == null ? 100L : Math.max(1L, interval.toMillis());
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() <= deadline) {
            if (Files.exists(path)) {
                return true;
            }
            Thread.sleep(intervalMillis);
        }
        return Files.exists(path);
    }

    /**
     * 在基础目录下创建日期目录。
     *
     * @param baseDir 基础目录
     * @return 日期目录
     * @throws IOException 创建失败时抛出
     */
    public static Path createDateDir(Path baseDir) throws IOException {
        return ensureDir(requireNonNullPath(baseDir).resolve(buildDateDir()));
    }

    /**
     * 清理过期文件。
     *
     * @param dir 目录路径
     * @param maxAge 最大保留时长
     * @return 删除文件数量
     * @throws IOException 清理失败时抛出
     */
    public static long clearExpiredFiles(Path dir, Duration maxAge) throws IOException {
        return cleanExpiredTempFiles(dir, maxAge);
    }

    /**
     * 要求路径存在。
     *
     * @param path 文件路径
     * @return 文件路径
     */
    public static Path requireExists(Path path) {
        return checkExists(path);
    }

    /**
     * 要求路径不存在。
     *
     * @param path 文件路径
     * @return 文件路径
     */
    public static Path requireNotExists(Path path) {
        return checkNotExists(path);
    }

    /**
     * 要求路径是普通文件。
     *
     * @param path 文件路径
     * @return 文件路径
     */
    public static Path requireFile(Path path) {
        return checkIsFile(path);
    }

    /**
     * 要求路径是目录。
     *
     * @param path 目录路径
     * @return 目录路径
     */
    public static Path requireDir(Path path) {
        return checkIsDir(path);
    }

    /**
     * 要求文件可读。
     *
     * @param path 文件路径
     * @return 文件路径
     */
    public static Path requireReadable(Path path) {
        return checkReadable(path);
    }

    /**
     * 要求文件可写。
     *
     * @param path 文件路径
     * @return 文件路径
     */
    public static Path requireWritable(Path path) {
        return checkWritable(path);
    }

    /**
     * 要求扩展名匹配。
     *
     * @param path 文件路径
     * @param extNames 允许的扩展名集合
     * @return 文件路径
     */
    public static Path requireExtName(Path path, Collection<String> extNames) {
        return checkExtNameAllowed(path, extNames);
    }

    /**
     * 要求文件大小不超过限制。
     *
     * @param path 文件路径
     * @param maxBytes 最大字节数
     * @return 文件路径
     * @throws IOException 读取大小失败时抛出
     */
    public static Path requireSizeLimit(Path path, long maxBytes) throws IOException {
        return checkSizeLimit(path, maxBytes);
    }

    /**
     * 要求目标路径安全。
     *
     * @param baseDir 基础目录
     * @param target 目标路径
     * @return 目标路径
     */
    public static Path requireSafePath(Path baseDir, Path target) {
        requireNonNullPath(baseDir);
        requireNonNullPath(target);
        Path base = baseDir.toAbsolutePath().normalize();
        Path resolved = target.toAbsolutePath().normalize();
        if (!resolved.startsWith(base)) {
            throw new IllegalArgumentException("路径不在允许目录内：" + target);
        }
        return resolved;
    }

    /**
     * 要求文件非空。
     *
     * @param path 文件路径
     * @return 文件路径
     * @throws IOException 读取大小失败时抛出
     */
    public static Path requireNonEmptyFile(Path path) throws IOException {
        requireFile(path);
        if (Files.size(path) <= 0) {
            throw new IllegalArgumentException("文件不能为空：" + path);
        }
        return path;
    }

    /**
     * 文件锁句柄，关闭时会同时释放锁和关闭通道。
     *
     * @author Ateng
     * @since 2026-04-29
     */
    public static final class FileLockHandle implements AutoCloseable {
        private final FileChannel channel;
        private final FileLock fileLock;

        private FileLockHandle(FileChannel channel, FileLock fileLock) {
            this.channel = channel;
            this.fileLock = fileLock;
        }

        /**
         * 获取底层文件锁。
         *
         * @return 文件锁
         */
        public FileLock fileLock() {
            return fileLock;
        }

        /**
         * 判断锁是否有效。
         *
         * @return 有效返回 true
         */
        public boolean isValid() {
            return fileLock.isValid();
        }

        /**
         * 释放锁并关闭文件通道。
         *
         * @throws IOException 关闭失败时抛出
         */
        @Override
        public void close() throws IOException {
            try {
                if (fileLock.isValid()) {
                    fileLock.release();
                }
            } finally {
                channel.close();
            }
        }
    }

    private static Path requireNonNullPath(Path path) {
        return Objects.requireNonNull(path, "路径不能为空");
    }

    private static Charset requireNonNullCharset(Charset charset) {
        return Objects.requireNonNull(charset, "字符集不能为空");
    }

    private static String normalizeExtName(String extName) {
        if (extName == null || extName.isBlank()) {
            return "";
        }
        String ext = extName.trim().toLowerCase(Locale.ROOT);
        return ext.startsWith(".") ? ext.substring(1) : ext;
    }

    private static Set<String> normalizeExtNames(Collection<String> extNames) {
        if (extNames == null || extNames.isEmpty()) {
            throw new IllegalArgumentException("扩展名集合不能为空");
        }
        Set<String> result = extNames.stream()
                .filter(Objects::nonNull)
                .map(FileUtil::normalizeExtName)
                .filter(ext -> !ext.isBlank())
                .collect(Collectors.toSet());
        if (result.isEmpty()) {
            throw new IllegalArgumentException("扩展名集合不能为空");
        }
        return result;
    }

    private static void ensurePosixSupported(Path path) {
        requireNonNullPath(path);
        if (Files.getFileAttributeView(path, PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS) == null) {
            throw new UnsupportedOperationException("当前文件系统不支持 POSIX 权限");
        }
    }

    private static String requireHash(String hash) {
        if (hash == null || hash.isBlank()) {
            throw new IllegalArgumentException("哈希值不能为空");
        }
        return hash.trim();
    }

    private static long checksum(Path path, Checksum checksum) throws IOException {
        try (InputStream input = newBufferedInputStream(requireFile(path));
             CheckedInputStream checkedInput = new CheckedInputStream(input, checksum)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            while (checkedInput.read(buffer) != -1) {
                // 读取即可触发校验值计算
            }
            return checkedInput.getChecksum().getValue();
        }
    }

    private static InputStream openClasspathResource(String resourceName) {
        if (resourceName == null || resourceName.isBlank()) {
            throw new IllegalArgumentException("资源名称不能为空");
        }
        String name = resourceName.startsWith("/") ? resourceName.substring(1) : resourceName;
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream input = loader == null ? FileUtil.class.getClassLoader().getResourceAsStream(name) : loader.getResourceAsStream(name);
        if (input == null) {
            throw new IllegalArgumentException("classpath 资源不存在：" + resourceName);
        }
        return input;
    }

    private static URI getClasspathResourceUri(String resourceName) throws URISyntaxException {
        if (resourceName == null || resourceName.isBlank()) {
            throw new IllegalArgumentException("资源名称不能为空");
        }
        String name = resourceName.startsWith("/") ? resourceName.substring(1) : resourceName;
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        var url = loader == null ? FileUtil.class.getClassLoader().getResource(name) : loader.getResource(name);
        if (url == null) {
            throw new IllegalArgumentException("classpath 资源不存在：" + resourceName);
        }
        return url.toURI();
    }

    private static Map<String, String> createMimeFallbacks() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("txt", "text/plain");
        map.put("csv", "text/csv");
        map.put("json", "application/json");
        map.put("xml", "application/xml");
        map.put("html", "text/html");
        map.put("css", "text/css");
        map.put("js", "application/javascript");
        map.put("pdf", "application/pdf");
        map.put("jpg", "image/jpeg");
        map.put("jpeg", "image/jpeg");
        map.put("png", "image/png");
        map.put("gif", "image/gif");
        map.put("svg", "image/svg+xml");
        map.put("zip", "application/zip");
        map.put("gz", "application/gzip");
        map.put("mp4", "video/mp4");
        map.put("mp3", "audio/mpeg");
        map.put("doc", "application/msword");
        map.put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        map.put("xls", "application/vnd.ms-excel");
        map.put("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        return Map.copyOf(map);
    }
}
