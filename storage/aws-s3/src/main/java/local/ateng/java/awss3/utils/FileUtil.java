package local.ateng.java.awss3.utils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
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
        if (baseDir == null || target == null) return null;
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
        if (path == null) return "";
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
        if (path == null) return "";
        String fileName = getFileName(path);
        int index = fileName.lastIndexOf(".");
        if (index > 0 && index < fileName.length() - 1) {
            return fileName.substring(index + 1);
        }
        return "";
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
        if (parent == null || child == null) return false;

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
        if (path == null) return "";
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
        deleteRecursively(tempDir, maxDeleteFileCount); // 复用安全递归删除
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
                walk.sorted((a, b) -> b.compareTo(a)) // 从子目录往上删
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
        if (paths == null || paths.isEmpty()) return streamMap;

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
        if (paths == null || paths.isEmpty()) return streamMap;

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
        if (paths == null || paths.isEmpty()) return streamMap;

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
        if (streams == null) return;
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


}

