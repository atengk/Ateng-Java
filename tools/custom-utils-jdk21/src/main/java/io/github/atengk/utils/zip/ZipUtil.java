package io.github.atengk.utils.zip;

import io.github.atengk.utils.zip.exception.ZipUtilException;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.model.enums.CompressionMethod;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorOutputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * 项目级压缩解压静态工具类，覆盖 ZIP、加密 ZIP、分卷 ZIP、TAR、GZIP、BZIP2、XZ、7Z、流式压缩、安全解压、条目管理和校验等常见场景。
 *
 * @author Ateng
 * @since 2026-04-29
 */
public final class ZipUtil {

    private static final Logger log = Logger.getLogger(ZipUtil.class.getName());
    private static final int BUFFER_SIZE = 8192;
    private static final long MIN_SPLIT_SIZE = 65_536L;
    private static final Map<String, TaskControl> TASKS = new ConcurrentHashMap<>();

    private ZipUtil() {
        throw new UnsupportedOperationException("ZipUtil 是静态工具类，禁止实例化");
    }

    /**
     * 压缩单个文件或目录为 ZIP。
     *
     * @param source  源文件或目录
     * @param zipPath 目标 ZIP 路径
     */
    public static void zip(Path source, Path zipPath) {
        zip(List.of(requirePath(source, "source")), zipPath);
    }

    /**
     * 压缩多个文件或目录为 ZIP。
     *
     * @param sources 源文件或目录集合
     * @param zipPath 目标 ZIP 路径
     */
    public static void zip(Collection<Path> sources, Path zipPath) {
        zipWithOptions(sources, zipPath, ZipOptions.defaults());
    }

    /**
     * 压缩目录为 ZIP。
     *
     * @param dir     源目录
     * @param zipPath 目标 ZIP 路径
     */
    public static void zipDir(Path dir, Path zipPath) {
        requireDirectory(dir, "dir");
        zip(dir, zipPath);
    }

    /**
     * 压缩单个文件为 ZIP。
     *
     * @param file    源文件
     * @param zipPath 目标 ZIP 路径
     */
    public static void zipFile(Path file, Path zipPath) {
        requireRegularFile(file, "file");
        zip(file, zipPath);
    }

    /**
     * 将文件或目录压缩到输出流。
     *
     * @param source 源文件或目录
     * @param out    输出流
     */
    public static void zipToStream(Path source, OutputStream out) {
        zipToStream(List.of(requirePath(source, "source")), out);
    }

    /**
     * 将多个文件或目录压缩到输出流。
     *
     * @param sources 源文件或目录集合
     * @param out     输出流
     */
    public static void zipToStream(Collection<Path> sources, OutputStream out) {
        requireNonEmpty(sources, "sources");
        Objects.requireNonNull(out, "out 不能为空");
        doZipToStream(sources, out, ZipOptions.defaults());
    }

    /**
     * 将字节内容作为一个条目写入 ZIP。
     *
     * @param entryName 条目名称
     * @param data      字节内容
     * @param zipPath   目标 ZIP 路径
     */
    public static void zipBytes(String entryName, byte[] data, Path zipPath) {
        Objects.requireNonNull(data, "data 不能为空");
        zipBytesToStream(Map.of(validateEntryName(entryName), data), openOutput(zipPath, true));
    }

    /**
     * 将文本内容作为一个条目写入 ZIP。
     *
     * @param entryName 条目名称
     * @param content   文本内容
     * @param zipPath   目标 ZIP 路径
     */
    public static void zipText(String entryName, String content, Path zipPath) {
        zipBytes(entryName, Objects.requireNonNull(content, "content 不能为空").getBytes(StandardCharsets.UTF_8), zipPath);
    }

    /**
     * 按选项压缩多个文件或目录为 ZIP。
     *
     * @param sources 源文件或目录集合
     * @param zipPath 目标 ZIP 路径
     * @param options 压缩选项
     */
    public static void zipWithOptions(Collection<Path> sources, Path zipPath, ZipOptions options) {
        requireNonEmpty(sources, "sources");
        requireParent(zipPath, true);
        ZipOptions actual = options == null ? ZipOptions.defaults() : options;
        if (Files.exists(zipPath) && !actual.overwrite()) {
            throw new ZipUtilException("目标 ZIP 已存在: " + zipPath);
        }
        if (actual.password() != null && actual.password().length > 0) {
            zipWithZip4jEncryption(sources, zipPath, actual.password(), actual.encryptionMethod(), actual.splitSize());
            return;
        }
        if (actual.splitSize() >= MIN_SPLIT_SIZE) {
            zipSplit(sources, zipPath, actual.splitSize());
            return;
        }
        try (OutputStream out = Files.newOutputStream(zipPath)) {
            doZipToStream(sources, out, actual);
        } catch (IOException e) {
            throw new ZipUtilException("压缩 ZIP 失败: " + zipPath, e);
        }
    }

    /**
     * 解压 ZIP 到目标目录。
     *
     * @param zipPath   ZIP 路径
     * @param targetDir 目标目录
     */
    public static void unzip(Path zipPath, Path targetDir) {
        unzipWithOptions(zipPath, targetDir, UnzipOptions.defaults());
    }

    /**
     * 从输入流解压 ZIP 到目标目录。
     *
     * @param in        ZIP 输入流
     * @param targetDir 目标目录
     */
    public static void unzip(InputStream in, Path targetDir) {
        Objects.requireNonNull(in, "in 不能为空");
        requireParent(targetDir.resolve("placeholder"), true);
        Path tmp = createTempZip("zip-stream-");
        try (OutputStream out = Files.newOutputStream(tmp)) {
            in.transferTo(out);
            unzip(tmp, targetDir);
        } catch (IOException e) {
            throw new ZipUtilException("从输入流解压 ZIP 失败", e);
        } finally {
            cleanQuietly(tmp);
        }
    }

    /**
     * 从字节数组解压 ZIP 到目标目录。
     *
     * @param data      ZIP 字节数组
     * @param targetDir 目标目录
     */
    public static void unzipBytes(byte[] data, Path targetDir) {
        Objects.requireNonNull(data, "data 不能为空");
        unzip(new ByteArrayInputStream(data), targetDir);
    }

    /**
     * 解压 ZIP 中的指定条目。
     *
     * @param zipPath    ZIP 路径
     * @param entryName  条目名称
     * @param targetPath 目标文件路径
     */
    public static void unzipEntry(Path zipPath, String entryName, Path targetPath) {
        requireRegularFile(zipPath, "zipPath");
        validateEntryName(entryName);
        requireParent(targetPath, true);
        try (java.util.zip.ZipFile zipFile = new java.util.zip.ZipFile(zipPath.toFile())) {
            ZipEntry entry = zipFile.getEntry(entryName);
            if (entry == null || entry.isDirectory()) {
                throw new ZipUtilException("ZIP 条目不存在或不是文件: " + entryName);
            }
            try (InputStream in = zipFile.getInputStream(entry)) {
                Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new ZipUtilException("解压指定条目失败: " + entryName, e);
        }
    }

    /**
     * 解压 ZIP 中的多个指定条目。
     *
     * @param zipPath    ZIP 路径
     * @param entryNames 条目名称集合
     * @param targetDir  目标目录
     */
    public static void unzipEntries(Path zipPath, Collection<String> entryNames, Path targetDir) {
        requireNonEmpty(entryNames, "entryNames");
        Set<String> names = entryNames.stream().map(ZipUtil::validateEntryName).collect(Collectors.toSet());
        unzipWithFilter(zipPath, targetDir, info -> names.contains(info.name()));
    }

    /**
     * 按选项解压 ZIP。
     *
     * @param zipPath   ZIP 路径
     * @param targetDir 目标目录
     * @param options   解压选项
     */
    public static void unzipWithOptions(Path zipPath, Path targetDir, UnzipOptions options) {
        requireRegularFile(zipPath, "zipPath");
        UnzipOptions actual = options == null ? UnzipOptions.defaults() : options;
        validateBeforeExtract(zipPath, ExtractOptions.fromUnzipOptions(actual));
        if (actual.password() != null && actual.password().length > 0) {
            unzipWithZip4jPassword(zipPath, targetDir, actual.password());
            return;
        }
        safeUnzipInternal(zipPath, targetDir, actual);
    }

    /**
     * 自动识别格式并解包。
     *
     * @param archivePath 压缩包路径
     * @param targetDir   目标目录
     */
    public static void extract(Path archivePath, Path targetDir) {
        extractWithOptions(archivePath, targetDir, ExtractOptions.defaults());
    }

    /**
     * 按选项自动识别格式并解包。
     *
     * @param archivePath 压缩包路径
     * @param targetDir   目标目录
     * @param options     解包选项
     */
    public static void extractWithOptions(Path archivePath, Path targetDir, ExtractOptions options) {
        requireRegularFile(archivePath, "archivePath");
        ExtractOptions actual = options == null ? ExtractOptions.defaults() : options;
        ArchiveFormat format = actual.format() == ArchiveFormat.UNKNOWN ? detectFormat(archivePath) : actual.format();
        switch (format) {
            case ZIP, JAR -> unzipWithOptions(archivePath, targetDir, actual.toUnzipOptions());
            case TAR -> untar(archivePath, targetDir, actual);
            case TAR_GZ, TGZ -> unTarWithCompressor(archivePath, targetDir, CompressorStreamFactory.GZIP, actual);
            case TAR_BZ2, TBZ2 -> unTarWithCompressor(archivePath, targetDir, CompressorStreamFactory.BZIP2, actual);
            case TAR_XZ, TXZ -> unTarWithCompressor(archivePath, targetDir, CompressorStreamFactory.XZ, actual);
            case GZIP ->
                    gunzip(archivePath, targetDir.resolve(stripKnownExtension(archivePath.getFileName().toString())));
            case BZIP2 ->
                    decompressSingle(archivePath, targetDir.resolve(stripKnownExtension(archivePath.getFileName().toString())), CompressorStreamFactory.BZIP2);
            case XZ ->
                    decompressSingle(archivePath, targetDir.resolve(stripKnownExtension(archivePath.getFileName().toString())), CompressorStreamFactory.XZ);
            case SEVEN_Z -> unSevenZip(archivePath, targetDir);
            default -> throw new ZipUtilException("不支持的压缩格式: " + archivePath);
        }
    }

    /**
     * 使用密码压缩文件或目录。
     *
     * @param source   源文件或目录
     * @param zipPath  目标 ZIP 路径
     * @param password 密码
     */
    public static void zipWithPassword(Path source, Path zipPath, char[] password) {
        zipWithAes(source, zipPath, password);
    }

    /**
     * 使用 AES 加密压缩文件或目录。
     *
     * @param source   源文件或目录
     * @param zipPath  目标 ZIP 路径
     * @param password 密码
     */
    public static void zipWithAes(Path source, Path zipPath, char[] password) {
        zipWithZip4jEncryption(source, zipPath, password, ZipEncryptionMethod.AES, 0L);
    }

    /**
     * 使用标准 ZIP 加密压缩文件或目录。
     *
     * @param source   源文件或目录
     * @param zipPath  目标 ZIP 路径
     * @param password 密码
     */
    public static void zipWithStandardEncryption(Path source, Path zipPath, char[] password) {
        zipWithZip4jEncryption(source, zipPath, password, ZipEncryptionMethod.STANDARD, 0L);
    }

    /**
     * 使用密码解压 ZIP。
     *
     * @param zipPath   ZIP 路径
     * @param targetDir 目标目录
     * @param password  密码
     */
    public static void unzipWithPassword(Path zipPath, Path targetDir, char[] password) {
        requirePassword(password);
        unzipWithZip4jPassword(zipPath, targetDir, password);
    }

    /**
     * 判断 ZIP 是否加密。
     *
     * @param zipPath ZIP 路径
     * @return 是否加密
     */
    public static boolean isEncryptedZip(Path zipPath) {
        requireRegularFile(zipPath, "zipPath");
        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            return zipFile.isEncrypted();
        } catch (IOException e) {
            throw new ZipUtilException("判断 ZIP 是否加密失败: " + zipPath, e);
        }
    }

    /**
     * 判断 ZIP 是否需要密码。
     *
     * @param zipPath ZIP 路径
     * @return 是否需要密码
     */
    public static boolean requiresPassword(Path zipPath) {
        return isEncryptedZip(zipPath);
    }

    /**
     * 校验 ZIP 密码是否可用。
     *
     * @param zipPath  ZIP 路径
     * @param password 密码
     * @return 密码是否可用
     */
    public static boolean checkPassword(Path zipPath, char[] password) {
        requirePassword(password);
        try {
            Path tmp = createTempDir("zip-password-check-");
            unzipWithZip4jPassword(zipPath, tmp, password);
            cleanQuietly(tmp);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * 修改加密 ZIP 密码。
     *
     * @param zipPath     ZIP 路径
     * @param oldPassword 原密码
     * @param newPassword 新密码
     */
    public static void changePassword(Path zipPath, char[] oldPassword, char[] newPassword) {
        requireRegularFile(zipPath, "zipPath");
        requirePassword(oldPassword);
        requirePassword(newPassword);
        Path tmpDir = createTempDir("zip-change-password-");
        Path tmpZip = createTempZip("zip-change-password-");
        try {
            unzipWithPassword(zipPath, tmpDir, oldPassword);
            zipWithAes(tmpDir, tmpZip, newPassword);
            Files.move(tmpZip, zipPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ZipUtilException("修改 ZIP 密码失败: " + zipPath, e);
        } finally {
            cleanQuietly(tmpDir);
            cleanQuietly(tmpZip);
        }
    }

    /**
     * 向 ZIP 添加一个文件或目录。
     *
     * @param zipPath ZIP 路径
     * @param file    文件或目录
     */
    public static void addEntry(Path zipPath, Path file) {
        addEntries(zipPath, List.of(file));
    }

    /**
     * 向 ZIP 批量添加文件或目录。
     *
     * @param zipPath ZIP 路径
     * @param files   文件或目录集合
     */
    public static void addEntries(Path zipPath, Collection<Path> files) {
        requireNonEmpty(files, "files");
        requireParent(zipPath, true);
        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            ZipParameters parameters = defaultZip4jParameters();
            for (Path file : files) {
                requirePath(file, "file");
                if (Files.isDirectory(file)) {
                    zipFile.addFolder(file.toFile(), parameters);
                } else {
                    zipFile.addFile(file.toFile(), parameters);
                }
            }
        } catch (IOException e) {
            throw new ZipUtilException("添加 ZIP 条目失败: " + zipPath, e);
        }
    }

    /**
     * 向 ZIP 添加文本条目。
     *
     * @param zipPath   ZIP 路径
     * @param entryName 条目名称
     * @param content   文本内容
     */
    public static void addTextEntry(Path zipPath, String entryName, String content) {
        addBytesEntry(zipPath, entryName, Objects.requireNonNull(content, "content 不能为空").getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 向 ZIP 添加字节条目。
     *
     * @param zipPath   ZIP 路径
     * @param entryName 条目名称
     * @param data      字节内容
     */
    public static void addBytesEntry(Path zipPath, String entryName, byte[] data) {
        requireParent(zipPath, true);
        validateEntryName(entryName);
        Objects.requireNonNull(data, "data 不能为空");
        try (ZipFile zipFile = new ZipFile(zipPath.toFile()); InputStream in = new ByteArrayInputStream(data)) {
            ZipParameters parameters = defaultZip4jParameters();
            parameters.setFileNameInZip(entryName);
            zipFile.addStream(in, parameters);
        } catch (IOException e) {
            throw new ZipUtilException("添加 ZIP 字节条目失败: " + entryName, e);
        }
    }

    /**
     * 替换 ZIP 中的指定条目。
     *
     * @param zipPath   ZIP 路径
     * @param entryName 条目名称
     * @param newFile   新文件
     */
    public static void replaceEntry(Path zipPath, String entryName, Path newFile) {
        if (entryExists(zipPath, entryName)) {
            removeEntry(zipPath, entryName);
        }
        try (InputStream in = Files.newInputStream(requireRegularFile(newFile, "newFile"))) {
            addStreamEntry(zipPath, entryName, in);
        } catch (IOException e) {
            throw new ZipUtilException("替换 ZIP 条目失败: " + entryName, e);
        }
    }

    /**
     * 删除 ZIP 中的指定条目。
     *
     * @param zipPath   ZIP 路径
     * @param entryName 条目名称
     */
    public static void removeEntry(Path zipPath, String entryName) {
        removeEntries(zipPath, List.of(validateEntryName(entryName)));
    }

    /**
     * 删除 ZIP 中的多个条目。
     *
     * @param zipPath    ZIP 路径
     * @param entryNames 条目名称集合
     */
    public static void removeEntries(Path zipPath, Collection<String> entryNames) {
        requireRegularFile(zipPath, "zipPath");
        requireNonEmpty(entryNames, "entryNames");
        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            zipFile.removeFiles(entryNames.stream().map(ZipUtil::validateEntryName).toList());
        } catch (IOException e) {
            throw new ZipUtilException("删除 ZIP 条目失败: " + zipPath, e);
        }
    }

    /**
     * 重命名 ZIP 中的条目。
     *
     * @param zipPath ZIP 路径
     * @param oldName 原条目名称
     * @param newName 新条目名称
     */
    public static void renameEntry(Path zipPath, String oldName, String newName) {
        requireRegularFile(zipPath, "zipPath");
        validateEntryName(oldName);
        validateEntryName(newName);
        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            zipFile.renameFile(oldName, newName);
        } catch (IOException e) {
            throw new ZipUtilException("重命名 ZIP 条目失败: " + oldName, e);
        }
    }

    /**
     * 判断 ZIP 中是否存在条目。
     *
     * @param zipPath   ZIP 路径
     * @param entryName 条目名称
     * @return 是否存在
     */
    public static boolean entryExists(Path zipPath, String entryName) {
        return containsEntry(zipPath, entryName);
    }

    /**
     * 复制 ZIP 条目到另一个 ZIP。
     *
     * @param sourceZip 源 ZIP
     * @param entryName 条目名称
     * @param targetZip 目标 ZIP
     */
    public static void copyEntry(Path sourceZip, String entryName, Path targetZip) {
        validateEntryName(entryName);
        try (java.util.zip.ZipFile zipFile = new java.util.zip.ZipFile(requireRegularFile(sourceZip, "sourceZip").toFile())) {
            ZipEntry entry = zipFile.getEntry(entryName);
            if (entry == null || entry.isDirectory()) {
                throw new ZipUtilException("ZIP 条目不存在或不是文件: " + entryName);
            }
            try (InputStream in = zipFile.getInputStream(entry)) {
                addStreamEntry(targetZip, entryName, in);
            }
        } catch (IOException e) {
            throw new ZipUtilException("复制 ZIP 条目失败: " + entryName, e);
        }
    }

    /**
     * 列出压缩包所有条目。
     *
     * @param archivePath 压缩包路径
     * @return 条目信息列表
     */
    public static List<ArchiveEntryInfo> listEntries(Path archivePath) {
        requireRegularFile(archivePath, "archivePath");
        ArchiveFormat format = detectFormat(archivePath);
        try {
            return switch (format) {
                case ZIP, JAR -> listZipEntries(archivePath);
                case TAR -> listTarEntries(Files.newInputStream(archivePath));
                case TAR_GZ, TGZ -> listTarEntries(newCompressorInput(archivePath, CompressorStreamFactory.GZIP));
                case TAR_BZ2, TBZ2 -> listTarEntries(newCompressorInput(archivePath, CompressorStreamFactory.BZIP2));
                case TAR_XZ, TXZ -> listTarEntries(newCompressorInput(archivePath, CompressorStreamFactory.XZ));
                case SEVEN_Z -> listSevenZEntries(archivePath);
                case GZIP, BZIP2, XZ -> List.of(singleCompressedEntry(archivePath, format));
                default -> throw new ZipUtilException("不支持读取条目的格式: " + archivePath);
            };
        } catch (IOException e) {
            throw new ZipUtilException("读取压缩包条目失败: " + archivePath, e);
        }
    }

    /**
     * 列出压缩包中的文件条目。
     *
     * @param archivePath 压缩包路径
     * @return 文件条目信息列表
     */
    public static List<ArchiveEntryInfo> listFileEntries(Path archivePath) {
        return listEntries(archivePath).stream().filter(info -> !info.directory()).toList();
    }

    /**
     * 列出压缩包中的目录条目。
     *
     * @param archivePath 压缩包路径
     * @return 目录条目信息列表
     */
    public static List<ArchiveEntryInfo> listDirEntries(Path archivePath) {
        return listEntries(archivePath).stream().filter(ArchiveEntryInfo::directory).toList();
    }

    /**
     * 获取指定条目信息。
     *
     * @param archivePath 压缩包路径
     * @param entryName   条目名称
     * @return 条目信息
     */
    public static Optional<ArchiveEntryInfo> getEntry(Path archivePath, String entryName) {
        validateEntryName(entryName);
        return listEntries(archivePath).stream().filter(info -> info.name().equals(entryName)).findFirst();
    }

    /**
     * 统计压缩包条目数量。
     *
     * @param archivePath 压缩包路径
     * @return 条目数量
     */
    public static int countEntries(Path archivePath) {
        return listEntries(archivePath).size();
    }

    /**
     * 获取压缩包摘要信息。
     *
     * @param archivePath 压缩包路径
     * @return 压缩包信息
     */
    public static ArchiveInfo getArchiveInfo(Path archivePath) {
        List<ArchiveEntryInfo> entries = listEntries(archivePath);
        long compressedSize = sizeOrZero(archivePath);
        long uncompressedSize = entries.stream().mapToLong(info -> Math.max(info.uncompressedSize(), 0)).sum();
        boolean encrypted = entries.stream().anyMatch(ArchiveEntryInfo::encrypted);
        return new ArchiveInfo(archivePath, detectFormat(archivePath), entries.size(), compressedSize, uncompressedSize, encrypted);
    }

    /**
     * 获取压缩包解压后总大小。
     *
     * @param archivePath 压缩包路径
     * @return 解压后总大小
     */
    public static long getTotalUncompressedSize(Path archivePath) {
        return listEntries(archivePath).stream().mapToLong(info -> Math.max(info.uncompressedSize(), 0)).sum();
    }

    /**
     * 获取压缩包压缩后总大小。
     *
     * @param archivePath 压缩包路径
     * @return 压缩后总大小
     */
    public static long getTotalCompressedSize(Path archivePath) {
        return listEntries(archivePath).stream().mapToLong(info -> Math.max(info.compressedSize(), 0)).sum();
    }

    /**
     * 判断压缩包是否包含指定条目。
     *
     * @param archivePath 压缩包路径
     * @param entryName   条目名称
     * @return 是否包含
     */
    public static boolean containsEntry(Path archivePath, String entryName) {
        validateEntryName(entryName);
        return listEntries(archivePath).stream().anyMatch(info -> info.name().equals(entryName));
    }

    /**
     * 按条件查找压缩包条目。
     *
     * @param archivePath 压缩包路径
     * @param predicate   过滤条件
     * @return 条目信息列表
     */
    public static List<ArchiveEntryInfo> findEntries(Path archivePath, Predicate<ArchiveEntryInfo> predicate) {
        Objects.requireNonNull(predicate, "predicate 不能为空");
        return listEntries(archivePath).stream().filter(predicate).toList();
    }

    /**
     * 打包为 TAR。
     *
     * @param source  源文件或目录
     * @param tarPath TAR 路径
     */
    public static void tar(Path source, Path tarPath) {
        requirePath(source, "source");
        requireParent(tarPath, true);
        try (OutputStream fos = Files.newOutputStream(tarPath);
             TarArchiveOutputStream tarOut = new TarArchiveOutputStream(new BufferedOutputStream(fos))) {
            tarOut.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
            writeTarEntries(source, source.getParent(), tarOut, ZipOptions.defaults());
        } catch (IOException e) {
            throw new ZipUtilException("TAR 打包失败: " + tarPath, e);
        }
    }

    /**
     * 解包 TAR。
     *
     * @param tarPath   TAR 路径
     * @param targetDir 目标目录
     */
    public static void untar(Path tarPath, Path targetDir) {
        untar(tarPath, targetDir, ExtractOptions.defaults());
    }

    /**
     * 压缩单文件为 GZIP。
     *
     * @param source   源文件
     * @param gzipPath GZIP 路径
     */
    public static void gzip(Path source, Path gzipPath) {
        compressSingle(source, gzipPath, CompressorStreamFactory.GZIP);
    }

    /**
     * 解压 GZIP 为单文件。
     *
     * @param gzipPath   GZIP 路径
     * @param targetPath 目标文件路径
     */
    public static void gunzip(Path gzipPath, Path targetPath) {
        decompressSingle(gzipPath, targetPath, CompressorStreamFactory.GZIP);
    }

    /**
     * 打包为 TAR.GZ。
     *
     * @param source    源文件或目录
     * @param tarGzPath TAR.GZ 路径
     */
    public static void tarGz(Path source, Path tarGzPath) {
        tarWithCompressor(source, tarGzPath, CompressorStreamFactory.GZIP);
    }

    /**
     * 解包 TAR.GZ。
     *
     * @param tarGzPath TAR.GZ 路径
     * @param targetDir 目标目录
     */
    public static void unTarGz(Path tarGzPath, Path targetDir) {
        unTarWithCompressor(tarGzPath, targetDir, CompressorStreamFactory.GZIP);
    }

    /**
     * 打包为 TAR.BZ2。
     *
     * @param source     源文件或目录
     * @param tarBz2Path TAR.BZ2 路径
     */
    public static void tarBz2(Path source, Path tarBz2Path) {
        tarWithCompressor(source, tarBz2Path, CompressorStreamFactory.BZIP2);
    }

    /**
     * 解包 TAR.BZ2。
     *
     * @param tarBz2Path TAR.BZ2 路径
     * @param targetDir  目标目录
     */
    public static void unTarBz2(Path tarBz2Path, Path targetDir) {
        unTarWithCompressor(tarBz2Path, targetDir, CompressorStreamFactory.BZIP2);
    }

    /**
     * 打包为 TAR.XZ。
     *
     * @param source    源文件或目录
     * @param tarXzPath TAR.XZ 路径
     */
    public static void tarXz(Path source, Path tarXzPath) {
        tarWithCompressor(source, tarXzPath, CompressorStreamFactory.XZ);
    }

    /**
     * 解包 TAR.XZ。
     *
     * @param tarXzPath TAR.XZ 路径
     * @param targetDir 目标目录
     */
    public static void unTarXz(Path tarXzPath, Path targetDir) {
        unTarWithCompressor(tarXzPath, targetDir, CompressorStreamFactory.XZ);
    }

    /**
     * 打包为 7Z。
     *
     * @param source     源文件或目录
     * @param sevenZPath 7Z 路径
     */
    public static void sevenZip(Path source, Path sevenZPath) {
        requirePath(source, "source");
        requireParent(sevenZPath, true);
        try (SevenZOutputFile out = new SevenZOutputFile(sevenZPath.toFile())) {
            writeSevenZEntries(source, source.getParent(), out, ZipOptions.defaults());
        } catch (IOException e) {
            throw new ZipUtilException("7Z 打包失败: " + sevenZPath, e);
        }
    }

    /**
     * 解包 7Z。
     *
     * @param sevenZPath 7Z 路径
     * @param targetDir  目标目录
     */
    public static void unSevenZip(Path sevenZPath, Path targetDir) {
        requireRegularFile(sevenZPath, "sevenZPath");
        requireParent(targetDir.resolve("placeholder"), true);
        try (SevenZFile sevenZFile = new SevenZFile(sevenZPath.toFile())) {
            SevenZArchiveEntry entry;
            while ((entry = sevenZFile.getNextEntry()) != null) {
                Path target = safeResolve(targetDir, entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                    continue;
                }
                requireParent(target, true);
                try (OutputStream out = Files.newOutputStream(target)) {
                    copySevenZEntry(sevenZFile, out);
                }
            }
        } catch (IOException e) {
            throw new ZipUtilException("7Z 解包失败: " + sevenZPath, e);
        }
    }

    /**
     * 按格式打包。
     *
     * @param source      源文件或目录
     * @param archivePath 目标压缩包路径
     * @param format      归档格式
     */
    public static void pack(Path source, Path archivePath, ArchiveFormat format) {
        switch (Objects.requireNonNull(format, "format 不能为空")) {
            case ZIP -> zip(source, archivePath);
            case TAR -> tar(source, archivePath);
            case TAR_GZ, TGZ -> tarGz(source, archivePath);
            case TAR_BZ2, TBZ2 -> tarBz2(source, archivePath);
            case TAR_XZ, TXZ -> tarXz(source, archivePath);
            case GZIP -> gzip(source, archivePath);
            case BZIP2 -> compressSingle(source, archivePath, CompressorStreamFactory.BZIP2);
            case XZ -> compressSingle(source, archivePath, CompressorStreamFactory.XZ);
            case SEVEN_Z -> sevenZip(source, archivePath);
            default -> throw new ZipUtilException("不支持打包格式: " + format);
        }
    }

    /**
     * 按格式解包。
     *
     * @param archivePath 压缩包路径
     * @param targetDir   目标目录
     * @param format      归档格式
     */
    public static void unpack(Path archivePath, Path targetDir, ArchiveFormat format) {
        extractWithOptions(archivePath, targetDir, ExtractOptions.defaults().withFormat(format));
    }

    /**
     * 自动识别压缩包格式。
     *
     * @param archivePath 压缩包路径
     * @return 格式枚举
     */
    public static ArchiveFormat detectFormat(Path archivePath) {
        requireRegularFile(archivePath, "archivePath");
        String name = archivePath.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".tar.gz")) return ArchiveFormat.TAR_GZ;
        if (name.endsWith(".tgz")) return ArchiveFormat.TGZ;
        if (name.endsWith(".tar.bz2")) return ArchiveFormat.TAR_BZ2;
        if (name.endsWith(".tbz2")) return ArchiveFormat.TBZ2;
        if (name.endsWith(".tar.xz")) return ArchiveFormat.TAR_XZ;
        if (name.endsWith(".txz")) return ArchiveFormat.TXZ;
        if (name.endsWith(".zip")) return ArchiveFormat.ZIP;
        if (name.endsWith(".jar")) return ArchiveFormat.JAR;
        if (name.endsWith(".tar")) return ArchiveFormat.TAR;
        if (name.endsWith(".gz")) return ArchiveFormat.GZIP;
        if (name.endsWith(".bz2")) return ArchiveFormat.BZIP2;
        if (name.endsWith(".xz")) return ArchiveFormat.XZ;
        if (name.endsWith(".7z")) return ArchiveFormat.SEVEN_Z;
        try (InputStream in = new BufferedInputStream(Files.newInputStream(archivePath))) {
            return detectFormat(in);
        } catch (IOException e) {
            throw new ZipUtilException("识别压缩格式失败: " + archivePath, e);
        }
    }

    /**
     * 从输入流识别压缩包格式。
     *
     * @param in 输入流，建议支持 mark/reset
     * @return 格式枚举
     */
    public static ArchiveFormat detectFormat(InputStream in) {
        Objects.requireNonNull(in, "in 不能为空");
        try {
            BufferedInputStream bin = in instanceof BufferedInputStream buffered ? buffered : new BufferedInputStream(in);
            bin.mark(600);
            byte[] header = bin.readNBytes(512);
            bin.reset();
            if (header.length >= 4 && header[0] == 'P' && header[1] == 'K') return ArchiveFormat.ZIP;
            if (header.length >= 2 && (header[0] & 0xFF) == 0x1F && (header[1] & 0xFF) == 0x8B)
                return ArchiveFormat.GZIP;
            if (header.length >= 3 && header[0] == 'B' && header[1] == 'Z' && header[2] == 'h')
                return ArchiveFormat.BZIP2;
            if (header.length >= 6 && (header[0] & 0xFF) == 0xFD && header[1] == '7' && header[2] == 'z' && header[3] == 'X' && header[4] == 'Z')
                return ArchiveFormat.XZ;
            if (header.length >= 6 && header[0] == '7' && header[1] == 'z' && (header[2] & 0xFF) == 0xBC && (header[3] & 0xFF) == 0xAF)
                return ArchiveFormat.SEVEN_Z;
            if (header.length > 263 && header[257] == 'u' && header[258] == 's' && header[259] == 't' && header[260] == 'a' && header[261] == 'r')
                return ArchiveFormat.TAR;
            return ArchiveFormat.UNKNOWN;
        } catch (IOException e) {
            throw new ZipUtilException("从输入流识别压缩格式失败", e);
        }
    }

    /**
     * 判断是否 ZIP。
     *
     * @param path 文件路径
     * @return 是否 ZIP
     */
    public static boolean isZip(Path path) {
        return detectFormat(path) == ArchiveFormat.ZIP;
    }

    /**
     * 判断是否 TAR。
     *
     * @param path 文件路径
     * @return 是否 TAR
     */
    public static boolean isTar(Path path) {
        return detectFormat(path) == ArchiveFormat.TAR;
    }

    /**
     * 判断是否 GZIP。
     *
     * @param path 文件路径
     * @return 是否 GZIP
     */
    public static boolean isGzip(Path path) {
        return detectFormat(path) == ArchiveFormat.GZIP;
    }

    /**
     * 判断是否 TAR.GZ。
     *
     * @param path 文件路径
     * @return 是否 TAR.GZ
     */
    public static boolean isTarGz(Path path) {
        ArchiveFormat format = detectFormat(path);
        return format == ArchiveFormat.TAR_GZ || format == ArchiveFormat.TGZ;
    }

    /**
     * 判断是否 7Z。
     *
     * @param path 文件路径
     * @return 是否 7Z
     */
    public static boolean isSevenZip(Path path) {
        return detectFormat(path) == ArchiveFormat.SEVEN_Z;
    }

    /**
     * 判断是否归档文件。
     *
     * @param path 文件路径
     * @return 是否归档文件
     */
    public static boolean isArchive(Path path) {
        return Set.of(ArchiveFormat.ZIP, ArchiveFormat.JAR, ArchiveFormat.TAR, ArchiveFormat.TAR_GZ, ArchiveFormat.TGZ,
                ArchiveFormat.TAR_BZ2, ArchiveFormat.TBZ2, ArchiveFormat.TAR_XZ, ArchiveFormat.TXZ, ArchiveFormat.SEVEN_Z).contains(detectFormat(path));
    }

    /**
     * 判断是否压缩文件。
     *
     * @param path 文件路径
     * @return 是否压缩文件
     */
    public static boolean isCompressed(Path path) {
        return detectFormat(path) != ArchiveFormat.UNKNOWN;
    }

    /**
     * 获取归档格式。
     *
     * @param path 文件路径
     * @return 归档格式
     */
    public static ArchiveFormat getArchiveFormat(Path path) {
        return detectFormat(path);
    }

    /**
     * 获取压缩流格式。
     *
     * @param path 文件路径
     * @return 压缩流格式
     */
    public static CompressorFormat getCompressorFormat(Path path) {
        return switch (detectFormat(path)) {
            case GZIP, TAR_GZ, TGZ -> CompressorFormat.GZIP;
            case BZIP2, TAR_BZ2, TBZ2 -> CompressorFormat.BZIP2;
            case XZ, TAR_XZ, TXZ -> CompressorFormat.XZ;
            default -> CompressorFormat.NONE;
        };
    }

    /**
     * 将动态条目压缩到输出流。
     *
     * @param entries 条目来源集合
     * @param out     输出流
     */
    public static void zipEntriesToStream(Collection<ZipEntrySource> entries, OutputStream out) {
        requireNonEmpty(entries, "entries");
        Objects.requireNonNull(out, "out 不能为空");
        try (ZipOutputStream zipOut = new ZipOutputStream(new BufferedOutputStream(out), StandardCharsets.UTF_8)) {
            for (ZipEntrySource source : entries) {
                validateEntryName(source.entryName());
                ZipEntry entry = new ZipEntry(source.entryName());
                if (source.lastModifiedTime() != null) {
                    entry.setLastModifiedTime(source.lastModifiedTime());
                }
                zipOut.putNextEntry(entry);
                try (InputStream in = source.inputStreamSupplier().open()) {
                    in.transferTo(zipOut);
                }
                zipOut.closeEntry();
            }
        } catch (IOException e) {
            throw new ZipUtilException("流式压缩动态条目失败", e);
        }
    }

    /**
     * 将多个输入流压缩到输出流。
     *
     * @param entries 条目名称与输入流映射
     * @param out     输出流
     */
    public static void zipInputStreams(Map<String, InputStream> entries, OutputStream out) {
        requireNonEmpty(entries.entrySet(), "entries");
        List<ZipEntrySource> sources = entries.entrySet().stream()
                .map(e -> new ZipEntrySource(e.getKey(), () -> e.getValue(), -1L, null))
                .toList();
        zipEntriesToStream(sources, out);
    }

    /**
     * 将多个字节内容压缩到输出流。
     *
     * @param entries 条目名称与字节内容映射
     * @param out     输出流
     */
    public static void zipBytesToStream(Map<String, byte[]> entries, OutputStream out) {
        requireNonEmpty(entries.entrySet(), "entries");
        List<ZipEntrySource> sources = entries.entrySet().stream()
                .map(e -> new ZipEntrySource(e.getKey(), () -> new ByteArrayInputStream(e.getValue()), e.getValue().length, null))
                .toList();
        zipEntriesToStream(sources, out);
    }

    /**
     * 将多个文本内容压缩到输出流。
     *
     * @param entries 条目名称与文本内容映射
     * @param out     输出流
     */
    public static void zipTextToStream(Map<String, String> entries, OutputStream out) {
        Map<String, byte[]> data = new LinkedHashMap<>();
        entries.forEach((name, text) -> data.put(name, Objects.requireNonNull(text, "text 不能为空").getBytes(StandardCharsets.UTF_8)));
        zipBytesToStream(data, out);
    }

    /**
     * 从输入流解包。
     *
     * @param in        输入流
     * @param targetDir 目标目录
     * @param format    格式
     */
    public static void extractFromStream(InputStream in, Path targetDir, ArchiveFormat format) {
        Objects.requireNonNull(in, "in 不能为空");
        Path tmp = createTempZip("extract-stream-");
        try (OutputStream out = Files.newOutputStream(tmp)) {
            in.transferTo(out);
            unpack(tmp, targetDir, format);
        } catch (IOException e) {
            throw new ZipUtilException("从输入流解包失败", e);
        } finally {
            cleanQuietly(tmp);
        }
    }

    /**
     * 创建分卷 ZIP。
     *
     * @param source    源文件或目录
     * @param zipPath   目标 ZIP 路径
     * @param splitSize 分卷大小，不能小于 65536 字节
     */
    public static void zipSplit(Path source, Path zipPath, long splitSize) {
        zipSplit(List.of(requirePath(source, "source")), zipPath, splitSize);
    }

    /**
     * 创建多文件分卷 ZIP。
     *
     * @param sources   源文件或目录集合
     * @param zipPath   目标 ZIP 路径
     * @param splitSize 分卷大小，不能小于 65536 字节
     */
    public static void zipSplit(Collection<Path> sources, Path zipPath, long splitSize) {
        requireNonEmpty(sources, "sources");
        if (splitSize < MIN_SPLIT_SIZE) {
            throw new IllegalArgumentException("splitSize 不能小于 " + MIN_SPLIT_SIZE);
        }
        requireParent(zipPath, true);
        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            ZipParameters parameters = defaultZip4jParameters();
            List<Path> list = sources.stream().map(p -> requirePath(p, "source")).toList();
            if (list.size() == 1 && Files.isDirectory(list.getFirst())) {
                zipFile.createSplitZipFileFromFolder(list.getFirst().toFile(), parameters, true, splitSize);
            } else {
                zipFile.createSplitZipFile(list.stream().map(Path::toFile).toList(), parameters, true, splitSize);
            }
        } catch (IOException e) {
            throw new ZipUtilException("创建分卷 ZIP 失败: " + zipPath, e);
        }
    }

    /**
     * 解压分卷 ZIP。
     *
     * @param zipPath   主 ZIP 路径
     * @param targetDir 目标目录
     */
    public static void unzipSplit(Path zipPath, Path targetDir) {
        unzipWithZip4jPassword(zipPath, targetDir, null);
    }

    /**
     * 使用密码解压分卷 ZIP。
     *
     * @param zipPath   主 ZIP 路径
     * @param targetDir 目标目录
     * @param password  密码
     */
    public static void unzipSplitWithPassword(Path zipPath, Path targetDir, char[] password) {
        unzipWithZip4jPassword(zipPath, targetDir, password);
    }

    /**
     * 判断是否分卷 ZIP。
     *
     * @param zipPath 主 ZIP 路径
     * @return 是否分卷
     */
    public static boolean isSplitZip(Path zipPath) {
        requireRegularFile(zipPath, "zipPath");
        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            return zipFile.isSplitArchive();
        } catch (IOException e) {
            throw new ZipUtilException("判断分卷 ZIP 失败: " + zipPath, e);
        }
    }

    /**
     * 列出分卷 ZIP 的所有分卷文件。
     *
     * @param zipPath 主 ZIP 路径
     * @return 分卷文件路径列表
     */
    public static List<Path> listSplitParts(Path zipPath) {
        requireRegularFile(zipPath, "zipPath");
        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            return zipFile.getSplitZipFiles().stream().map(part -> Path.of(part.toString())).toList();
        } catch (IOException e) {
            throw new ZipUtilException("列出分卷 ZIP 文件失败: " + zipPath, e);
        }
    }

    /**
     * 校验分卷 ZIP 文件是否完整存在。
     *
     * @param zipPath 主 ZIP 路径
     * @return 是否完整
     */
    public static boolean validateSplitParts(Path zipPath) {
        return listSplitParts(zipPath).stream().allMatch(Files::isRegularFile);
    }

    /**
     * 带进度压缩。
     *
     * @param source   源文件或目录
     * @param zipPath  目标 ZIP 路径
     * @param listener 进度监听器
     */
    public static void zipWithProgress(Path source, Path zipPath, ProgressListener listener) {
        zipWithOptions(List.of(source), zipPath, ZipOptions.defaults().withProgressListener(listener));
    }

    /**
     * 带进度解压。
     *
     * @param zipPath   ZIP 路径
     * @param targetDir 目标目录
     * @param listener  进度监听器
     */
    public static void unzipWithProgress(Path zipPath, Path targetDir, ProgressListener listener) {
        unzipWithOptions(zipPath, targetDir, UnzipOptions.defaults().withProgressListener(listener));
    }

    /**
     * 带进度通用解包。
     *
     * @param archivePath 压缩包路径
     * @param targetDir   目标目录
     * @param listener    进度监听器
     */
    public static void extractWithProgress(Path archivePath, Path targetDir, ProgressListener listener) {
        extractWithOptions(archivePath, targetDir, ExtractOptions.defaults().withProgressListener(listener));
    }

    /**
     * 估算文件集合处理总字节数。
     *
     * @param sources 源文件或目录集合
     * @return 总字节数
     */
    public static long estimateWorkload(Collection<Path> sources) {
        requireNonEmpty(sources, "sources");
        return sources.stream().mapToLong(ZipUtil::totalFileSize).sum();
    }

    /**
     * 查询任务进度。
     *
     * @param taskId 任务编号
     * @return 进度百分比
     */
    public static OptionalInt getProgress(String taskId) {
        TaskControl control = TASKS.get(taskId);
        return control == null ? OptionalInt.empty() : OptionalInt.of(control.percent);
    }

    /**
     * 取消任务。
     *
     * @param taskId 任务编号
     */
    public static void cancelTask(String taskId) {
        TaskControl control = TASKS.get(taskId);
        if (control != null) {
            control.cancelled = true;
        }
    }

    /**
     * 暂停任务。
     *
     * @param taskId 任务编号
     */
    public static void pauseTask(String taskId) {
        TaskControl control = TASKS.get(taskId);
        if (control != null) {
            control.paused = true;
        }
    }

    /**
     * 恢复任务。
     *
     * @param taskId 任务编号
     */
    public static void resumeTask(String taskId) {
        TaskControl control = TASKS.get(taskId);
        if (control != null) {
            control.paused = false;
        }
    }

    /**
     * 安全解压 ZIP。
     *
     * @param zipPath   ZIP 路径
     * @param targetDir 目标目录
     */
    public static void safeUnzip(Path zipPath, Path targetDir) {
        unzipWithOptions(zipPath, targetDir, UnzipOptions.defaults());
    }

    /**
     * 安全解包。
     *
     * @param archivePath 压缩包路径
     * @param targetDir   目标目录
     */
    public static void safeExtract(Path archivePath, Path targetDir) {
        extractWithOptions(archivePath, targetDir, ExtractOptions.defaults());
    }

    /**
     * 解包前安全校验。
     *
     * @param archivePath 压缩包路径
     * @param options     解包选项
     */
    public static void validateBeforeExtract(Path archivePath, ExtractOptions options) {
        requireRegularFile(archivePath, "archivePath");
        ExtractOptions actual = options == null ? ExtractOptions.defaults() : options;
        SecurityOptions security = actual.securityOptions();
        List<ArchiveEntryInfo> entries = listEntriesIfSupported(archivePath);
        if (entries.isEmpty()) {
            return;
        }
        if (entries.size() > security.maxEntryCount()) {
            throw new ZipUtilException("压缩包条目数量超过限制: " + entries.size());
        }
        long totalSize = 0L;
        for (ArchiveEntryInfo entry : entries) {
            validateEntryBySecurity(entry, security);
            totalSize += Math.max(entry.uncompressedSize(), 0L);
        }
        if (totalSize > security.maxTotalSize()) {
            throw new ZipUtilException("压缩包解压后大小超过限制: " + totalSize);
        }
    }

    /**
     * 校验条目名称是否合法。
     *
     * @param entryName 条目名称
     * @return 合法条目名称
     */
    public static String validateEntryName(String entryName) {
        if (entryName == null || entryName.isBlank()) {
            throw new IllegalArgumentException("entryName 不能为空");
        }
        if (isZipSlipEntry(entryName)) {
            throw new ZipUtilException("条目名称存在路径穿越风险: " + entryName);
        }
        return entryName.replace('\\', '/');
    }

    /**
     * 安全解析解压目标路径。
     *
     * @param targetDir 目标目录
     * @param entryName 条目名称
     * @return 安全目标路径
     */
    public static Path safeResolve(Path targetDir, String entryName) {
        requirePathObject(targetDir, "targetDir");
        String safeName = validateEntryName(entryName);
        Path normalizedTargetDir = targetDir.toAbsolutePath().normalize();
        Path resolved = normalizedTargetDir.resolve(safeName).normalize();
        if (!resolved.startsWith(normalizedTargetDir)) {
            throw new ZipUtilException("条目路径越界: " + entryName);
        }
        return resolved;
    }

    /**
     * 判断条目是否存在路径穿越风险。
     *
     * @param entryName 条目名称
     * @return 是否风险条目
     */
    public static boolean isZipSlipEntry(String entryName) {
        if (entryName == null || entryName.isBlank()) {
            return true;
        }
        String normalized = entryName.replace('\\', '/');
        return normalized.startsWith("/") || normalized.contains("../") || normalized.equals("..") || normalized.contains(":");
    }

    /**
     * 校验最大条目数。
     *
     * @param archivePath 压缩包路径
     * @param maxCount    最大条目数
     */
    public static void checkMaxEntryCount(Path archivePath, int maxCount) {
        if (countEntries(archivePath) > maxCount) {
            throw new ZipUtilException("压缩包条目数量超过限制: " + maxCount);
        }
    }

    /**
     * 校验最大解压后大小。
     *
     * @param archivePath 压缩包路径
     * @param maxSize     最大字节数
     */
    public static void checkMaxUncompressedSize(Path archivePath, long maxSize) {
        if (getTotalUncompressedSize(archivePath) > maxSize) {
            throw new ZipUtilException("压缩包解压后大小超过限制: " + maxSize);
        }
    }

    /**
     * 校验最大压缩比。
     *
     * @param archivePath 压缩包路径
     * @param maxRatio    最大压缩比
     */
    public static void checkCompressionRatio(Path archivePath, BigDecimal maxRatio) {
        Objects.requireNonNull(maxRatio, "maxRatio 不能为空");
        ArchiveInfo info = getArchiveInfo(archivePath);
        if (info.compressedSize() <= 0) {
            return;
        }
        BigDecimal ratio = BigDecimal.valueOf(info.uncompressedSize()).divide(BigDecimal.valueOf(info.compressedSize()), 4, RoundingMode.HALF_UP);
        if (ratio.compareTo(maxRatio) > 0) {
            throw new ZipUtilException("压缩比超过限制: " + ratio);
        }
    }

    /**
     * 校验允许的文件扩展名。
     *
     * @param archivePath 压缩包路径
     * @param extensions  允许扩展名集合
     */
    public static void checkAllowedExtensions(Path archivePath, Set<String> extensions) {
        Set<String> normalized = normalizeExtensions(extensions);
        for (ArchiveEntryInfo entry : listFileEntries(archivePath)) {
            if (!normalized.contains(extension(entry.name()))) {
                throw new ZipUtilException("压缩包存在不允许的文件类型: " + entry.name());
            }
        }
    }

    /**
     * 校验禁止的文件扩展名。
     *
     * @param archivePath 压缩包路径
     * @param extensions  禁止扩展名集合
     */
    public static void checkBlockedExtensions(Path archivePath, Set<String> extensions) {
        Set<String> normalized = normalizeExtensions(extensions);
        for (ArchiveEntryInfo entry : listFileEntries(archivePath)) {
            if (normalized.contains(extension(entry.name()))) {
                throw new ZipUtilException("压缩包存在禁止的文件类型: " + entry.name());
            }
        }
    }

    /**
     * 清理条目名称。
     *
     * @param entryName 条目名称
     * @return 清理后的条目名称
     */
    public static String sanitizeEntryName(String entryName) {
        if (entryName == null || entryName.isBlank()) {
            return "unnamed";
        }
        String cleaned = entryName.replace('\\', '/').replaceAll("(^/+)|(/+$)", "");
        cleaned = cleaned.replace("..", "_").replace(":", "_");
        return cleaned.isBlank() ? "unnamed" : cleaned;
    }

    /**
     * 清理文件名。
     *
     * @param fileName 文件名
     * @return 清理后的文件名
     */
    public static String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "unnamed";
        }
        String cleaned = fileName.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        return cleaned.isBlank() ? "unnamed" : cleaned;
    }

    /**
     * 按文件过滤器压缩。
     *
     * @param source  源文件或目录
     * @param zipPath 目标 ZIP 路径
     * @param filter  文件过滤器
     */
    public static void zipWithFilter(Path source, Path zipPath, Predicate<Path> filter) {
        zipWithOptions(List.of(source), zipPath, ZipOptions.defaults().withEntryFilter(filter));
    }

    /**
     * 按条目过滤器解压 ZIP。
     *
     * @param zipPath   ZIP 路径
     * @param targetDir 目标目录
     * @param filter    条目过滤器
     */
    public static void unzipWithFilter(Path zipPath, Path targetDir, Predicate<ArchiveEntryInfo> filter) {
        unzipWithOptions(zipPath, targetDir, UnzipOptions.defaults().withEntryFilter(filter));
    }

    /**
     * 按条目过滤器通用解包。
     *
     * @param archivePath 压缩包路径
     * @param targetDir   目标目录
     * @param filter      条目过滤器
     */
    public static void extractWithFilter(Path archivePath, Path targetDir, Predicate<ArchiveEntryInfo> filter) {
        extractWithOptions(archivePath, targetDir, ExtractOptions.defaults().withEntryFilter(filter));
    }

    /**
     * 按条目名称映射器压缩。
     *
     * @param source  源文件或目录
     * @param zipPath 目标 ZIP 路径
     * @param mapper  条目名称映射器
     */
    public static void zipWithMapper(Path source, Path zipPath, EntryNameMapper mapper) {
        zipWithOptions(List.of(source), zipPath, ZipOptions.defaults().withEntryNameMapper(mapper));
    }

    /**
     * 解压时去除公共根目录。
     *
     * @param archivePath 压缩包路径
     * @param targetDir   目标目录
     */
    public static void stripRootDirectory(Path archivePath, Path targetDir) {
        extractWithOptions(archivePath, targetDir, ExtractOptions.defaults().withStripRootDir(true));
    }

    /**
     * 构造包含扩展名过滤器。
     *
     * @param extensions 扩展名集合
     * @return 文件过滤器
     */
    public static Predicate<Path> includeExtensions(Set<String> extensions) {
        Set<String> normalized = normalizeExtensions(extensions);
        return path -> Files.isDirectory(path) || normalized.contains(extension(path.getFileName().toString()));
    }

    /**
     * 构造排除扩展名过滤器。
     *
     * @param extensions 扩展名集合
     * @return 文件过滤器
     */
    public static Predicate<Path> excludeExtensions(Set<String> extensions) {
        Set<String> normalized = normalizeExtensions(extensions);
        return path -> Files.isDirectory(path) || !normalized.contains(extension(path.getFileName().toString()));
    }

    /**
     * 构造排除隐藏文件过滤器。
     *
     * @return 文件过滤器
     */
    public static Predicate<Path> excludeHiddenFiles() {
        return path -> {
            try {
                return !Files.isHidden(path);
            } catch (IOException e) {
                return true;
            }
        };
    }

    /**
     * 构造排除空目录过滤器。
     *
     * @return 文件过滤器
     */
    public static Predicate<Path> excludeEmptyDirectories() {
        return path -> !Files.isDirectory(path) || hasChildren(path);
    }

    /**
     * 指定冲突策略解压 ZIP。
     *
     * @param zipPath   ZIP 路径
     * @param targetDir 目标目录
     * @param strategy  冲突策略
     */
    public static void unzipWithConflictStrategy(Path zipPath, Path targetDir, ConflictStrategy strategy) {
        unzipWithOptions(zipPath, targetDir, UnzipOptions.defaults().withConflictStrategy(strategy));
    }

    /**
     * 指定冲突策略通用解包。
     *
     * @param archivePath 压缩包路径
     * @param targetDir   目标目录
     * @param strategy    冲突策略
     */
    public static void extractWithConflictStrategy(Path archivePath, Path targetDir, ConflictStrategy strategy) {
        extractWithOptions(archivePath, targetDir, ExtractOptions.defaults().withConflictStrategy(strategy));
    }

    /**
     * 根据冲突策略解析目标路径。
     *
     * @param targetPath 原目标路径
     * @param strategy   冲突策略
     * @return 解析后的路径
     */
    public static Path resolveConflict(Path targetPath, ConflictStrategy strategy) {
        requirePathObject(targetPath, "targetPath");
        ConflictStrategy actual = strategy == null ? ConflictStrategy.FAIL : strategy;
        if (!Files.exists(targetPath)) {
            return targetPath;
        }
        return switch (actual) {
            case OVERWRITE -> targetPath;
            case SKIP -> null;
            case RENAME -> renameIfExists(targetPath);
            case BACKUP -> backupIfExists(targetPath);
            case FAIL -> throw new ZipUtilException("目标文件已存在: " + targetPath);
        };
    }

    /**
     * 文件存在时生成不冲突的新路径。
     *
     * @param targetPath 原路径
     * @return 新路径
     */
    public static Path renameIfExists(Path targetPath) {
        if (!Files.exists(targetPath)) {
            return targetPath;
        }
        String name = targetPath.getFileName().toString();
        String base = baseName(name);
        String ext = suffix(name);
        Path parent = targetPath.getParent();
        int i = 1;
        Path candidate;
        do {
            candidate = parent.resolve(base + "(" + i++ + ")" + ext);
        } while (Files.exists(candidate));
        return candidate;
    }

    /**
     * 文件存在时备份并返回原路径。
     *
     * @param targetPath 原路径
     * @return 原路径
     */
    public static Path backupIfExists(Path targetPath) {
        if (!Files.exists(targetPath)) {
            return targetPath;
        }
        Path backup = renameIfExists(targetPath.resolveSibling(targetPath.getFileName() + ".bak"));
        try {
            Files.move(targetPath, backup, StandardCopyOption.REPLACE_EXISTING);
            return targetPath;
        } catch (IOException e) {
            throw new ZipUtilException("备份冲突文件失败: " + targetPath, e);
        }
    }

    /**
     * 文件存在时返回空路径表示跳过。
     *
     * @param targetPath 原路径
     * @return 不存在返回原路径，存在返回 null
     */
    public static Path skipIfExists(Path targetPath) {
        return Files.exists(targetPath) ? null : targetPath;
    }

    /**
     * 文件存在时直接使用原路径覆盖。
     *
     * @param targetPath 原路径
     * @return 原路径
     */
    public static Path overwriteIfExists(Path targetPath) {
        return targetPath;
    }

    /**
     * 创建临时 ZIP 文件。
     *
     * @param prefix 文件名前缀
     * @return 临时 ZIP 路径
     */
    public static Path createTempZip(String prefix) {
        try {
            return Files.createTempFile(prefix == null ? "zip-" : prefix, ".zip");
        } catch (IOException e) {
            throw new ZipUtilException("创建临时 ZIP 失败", e);
        }
    }

    /**
     * 创建临时目录。
     *
     * @param prefix 目录名前缀
     * @return 临时目录路径
     */
    public static Path createTempDir(String prefix) {
        try {
            return Files.createTempDirectory(prefix == null ? "zip-" : prefix);
        } catch (IOException e) {
            throw new ZipUtilException("创建临时目录失败", e);
        }
    }

    /**
     * 压缩到临时 ZIP。
     *
     * @param source 源文件或目录
     * @return 临时 ZIP 路径
     */
    public static Path zipToTemp(Path source) {
        Path tempZip = createTempZip("zip-temp-");
        zip(source, tempZip);
        return tempZip;
    }

    /**
     * 解包到临时目录。
     *
     * @param archivePath 压缩包路径
     * @return 临时目录路径
     */
    public static Path extractToTemp(Path archivePath) {
        Path tempDir = createTempDir("zip-extract-");
        extract(archivePath, tempDir);
        return tempDir;
    }

    /**
     * 清理临时文件或目录。
     *
     * @param path 文件或目录路径
     */
    public static void cleanTemp(Path path) {
        deleteIfExists(path);
    }

    /**
     * 静默清理文件或目录。
     *
     * @param path 文件或目录路径
     */
    public static void cleanQuietly(Path path) {
        try {
            deleteIfExists(path);
        } catch (RuntimeException ignored) {
            log.fine("静默清理失败: " + path);
        }
    }

    /**
     * 删除文件或目录。
     *
     * @param path 文件或目录路径
     */
    public static void deleteIfExists(Path path) {
        if (path == null || !Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
            return;
        }
        if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
            deleteDirectory(path);
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new ZipUtilException("删除文件失败: " + path, e);
        }
    }

    /**
     * 删除目录。
     *
     * @param dir 目录路径
     */
    public static void deleteDirectory(Path dir) {
        if (dir == null || !Files.exists(dir, LinkOption.NOFOLLOW_LINKS)) {
            return;
        }
        try (Stream<Path> stream = Files.walk(dir)) {
            List<Path> paths = stream.sorted(Comparator.reverseOrder()).toList();
            for (Path path : paths) {
                Files.deleteIfExists(path);
            }
        } catch (IOException e) {
            throw new ZipUtilException("删除目录失败: " + dir, e);
        }
    }

    /**
     * 失败时回滚删除目标路径。
     *
     * @param path 需要回滚删除的路径
     */
    public static void rollbackOnFailure(Path path) {
        cleanQuietly(path);
    }

    /**
     * 测试压缩包是否可读。
     *
     * @param archivePath 压缩包路径
     * @return 是否可读
     */
    public static boolean testArchive(Path archivePath) {
        try {
            validateArchive(archivePath);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * 测试 ZIP 是否可读。
     *
     * @param zipPath ZIP 路径
     * @return 是否可读
     */
    public static boolean testZip(Path zipPath) {
        return testArchive(zipPath);
    }

    /**
     * 校验压缩包。
     *
     * @param archivePath 压缩包路径
     * @return 校验结果
     */
    public static ArchiveValidateResult validateArchive(Path archivePath) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<ArchiveEntryInfo> entries = List.of();
        try {
            entries = listEntries(archivePath);
            if (entries.isEmpty()) {
                warnings.add("压缩包没有条目");
            }
            validateBeforeExtract(archivePath, ExtractOptions.defaults());
        } catch (RuntimeException e) {
            errors.add(e.getMessage());
        }
        long totalSize = entries.stream().mapToLong(e -> Math.max(e.uncompressedSize(), 0)).sum();
        return new ArchiveValidateResult(errors.isEmpty(), errors, warnings, entries.size(), totalSize);
    }

    /**
     * 校验 ZIP。
     *
     * @param zipPath ZIP 路径
     * @return 校验结果
     */
    public static ArchiveValidateResult validateZip(Path zipPath) {
        return validateArchive(zipPath);
    }

    /**
     * 校验条目是否存在。
     *
     * @param archivePath 压缩包路径
     * @param entryName   条目名称
     */
    public static void verifyEntryExists(Path archivePath, String entryName) {
        if (!containsEntry(archivePath, entryName)) {
            throw new ZipUtilException("压缩包条目不存在: " + entryName);
        }
    }

    /**
     * 校验条目数量。
     *
     * @param archivePath   压缩包路径
     * @param expectedCount 期望数量
     */
    public static void verifyEntryCount(Path archivePath, int expectedCount) {
        int actual = countEntries(archivePath);
        if (actual != expectedCount) {
            throw new ZipUtilException("压缩包条目数量不匹配，期望: " + expectedCount + ", 实际: " + actual);
        }
    }

    /**
     * 校验文件摘要。
     *
     * @param file             文件路径
     * @param expectedChecksum 期望摘要
     */
    public static void verifyChecksum(Path file, String expectedChecksum) {
        String actual = sha256(file);
        if (!actual.equalsIgnoreCase(Objects.requireNonNull(expectedChecksum, "expectedChecksum 不能为空"))) {
            throw new ZipUtilException("文件 SHA-256 摘要不匹配: " + file);
        }
    }

    /**
     * 计算 CRC32。
     *
     * @param file 文件路径
     * @return CRC32 十六进制值
     */
    public static String crc32(Path file) {
        requireRegularFile(file, "file");
        CRC32 crc32 = new CRC32();
        try (InputStream in = Files.newInputStream(file)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int len;
            while ((len = in.read(buffer)) != -1) {
                crc32.update(buffer, 0, len);
            }
            return Long.toHexString(crc32.getValue());
        } catch (IOException e) {
            throw new ZipUtilException("计算 CRC32 失败: " + file, e);
        }
    }

    /**
     * 计算 MD5。
     *
     * @param file 文件路径
     * @return MD5 十六进制值
     */
    public static String md5(Path file) {
        return digest(file, "MD5");
    }

    /**
     * 计算 SHA-256。
     *
     * @param file 文件路径
     * @return SHA-256 十六进制值
     */
    public static String sha256(Path file) {
        return digest(file, "SHA-256");
    }

    /**
     * 指定编码压缩 ZIP。
     *
     * @param source  源文件或目录
     * @param zipPath 目标 ZIP 路径
     * @param charset 文件名编码
     */
    public static void zipWithCharset(Path source, Path zipPath, Charset charset) {
        zipWithOptions(List.of(source), zipPath, ZipOptions.defaults().withCharset(normalizeEncoding(charset)));
    }

    /**
     * 指定编码解压 ZIP。
     *
     * @param zipPath   ZIP 路径
     * @param targetDir 目标目录
     * @param charset   文件名编码
     */
    public static void unzipWithCharset(Path zipPath, Path targetDir, Charset charset) {
        unzipWithOptions(zipPath, targetDir, UnzipOptions.defaults().withCharset(normalizeEncoding(charset)));
    }

    /**
     * 设置 ZIP 注释。
     *
     * @param zipPath ZIP 路径
     * @param comment 注释
     */
    public static void setZipComment(Path zipPath, String comment) {
        requireRegularFile(zipPath, "zipPath");
        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            zipFile.setComment(comment == null ? "" : comment);
        } catch (IOException e) {
            throw new ZipUtilException("设置 ZIP 注释失败: " + zipPath, e);
        }
    }

    /**
     * 获取 ZIP 注释。
     *
     * @param zipPath ZIP 路径
     * @return 注释
     */
    public static String getZipComment(Path zipPath) {
        requireRegularFile(zipPath, "zipPath");
        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            return zipFile.getComment();
        } catch (IOException e) {
            throw new ZipUtilException("获取 ZIP 注释失败: " + zipPath, e);
        }
    }

    /**
     * 设置 ZIP 条目注释。
     *
     * @param zipPath   ZIP 路径
     * @param entryName 条目名称
     * @param comment   注释
     */
    public static void setEntryComment(Path zipPath, String entryName, String comment) {
        requireRegularFile(zipPath, "zipPath");
        validateEntryName(entryName);
        Path tempZip = createTempZip("zip-entry-comment-");
        boolean found = false;
        try (java.util.zip.ZipFile source = new java.util.zip.ZipFile(zipPath.toFile(), StandardCharsets.UTF_8);
             ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(tempZip)), StandardCharsets.UTF_8)) {
            Enumeration<? extends ZipEntry> entries = source.entries();
            while (entries.hasMoreElements()) {
                ZipEntry oldEntry = entries.nextElement();
                ZipEntry newEntry = copyZipEntryMetadata(oldEntry);
                if (oldEntry.getName().equals(entryName)) {
                    newEntry.setComment(comment == null ? "" : comment);
                    found = true;
                }
                out.putNextEntry(newEntry);
                if (!oldEntry.isDirectory()) {
                    try (InputStream in = source.getInputStream(oldEntry)) {
                        in.transferTo(out);
                    }
                }
                out.closeEntry();
            }
            if (!found) {
                throw new ZipUtilException("ZIP 条目不存在: " + entryName);
            }
        } catch (IOException e) {
            throw new ZipUtilException("设置 ZIP 条目注释失败: " + entryName, e);
        }
        try {
            Files.move(tempZip, zipPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("已更新 ZIP 条目注释: " + entryName);
        } catch (IOException e) {
            throw new ZipUtilException("替换 ZIP 文件失败: " + zipPath, e);
        } finally {
            cleanQuietly(tempZip);
        }
    }

    /**
     * 获取 ZIP 条目注释。
     *
     * @param zipPath   ZIP 路径
     * @param entryName 条目名称
     * @return 条目注释
     */
    public static String getEntryComment(Path zipPath, String entryName) {
        requireRegularFile(zipPath, "zipPath");
        validateEntryName(entryName);
        try (java.util.zip.ZipFile zipFile = new java.util.zip.ZipFile(zipPath.toFile(), StandardCharsets.UTF_8)) {
            ZipEntry entry = zipFile.getEntry(entryName);
            return entry == null ? null : entry.getComment();
        } catch (IOException e) {
            throw new ZipUtilException("获取 ZIP 条目注释失败: " + entryName, e);
        }
    }

    /**
     * 规范化编码，空值默认 UTF-8。
     *
     * @param charset 编码
     * @return 编码
     */
    public static Charset normalizeEncoding(Charset charset) {
        return charset == null ? StandardCharsets.UTF_8 : charset;
    }

    /**
     * 每个源文件或目录单独压缩。
     *
     * @param sources   源文件或目录集合
     * @param targetDir 目标目录
     * @return 生成的 ZIP 路径列表
     */
    public static List<Path> zipEach(Collection<Path> sources, Path targetDir) {
        requireNonEmpty(sources, "sources");
        requireParent(targetDir.resolve("placeholder"), true);
        List<Path> result = new ArrayList<>();
        for (Path source : sources) {
            Path zipPath = targetDir.resolve(baseName(source.getFileName().toString()) + ".zip");
            zip(source, zipPath);
            result.add(zipPath);
        }
        return result;
    }

    /**
     * 批量解压 ZIP。
     *
     * @param zipFiles  ZIP 文件集合
     * @param targetDir 目标目录
     */
    public static void unzipEach(Collection<Path> zipFiles, Path targetDir) {
        requireNonEmpty(zipFiles, "zipFiles");
        for (Path zipFile : zipFiles) {
            unzip(zipFile, targetDir.resolve(baseName(zipFile.getFileName().toString())));
        }
    }

    /**
     * 批量解包。
     *
     * @param archiveFiles 压缩包集合
     * @param targetDir    目标目录
     */
    public static void extractEach(Collection<Path> archiveFiles, Path targetDir) {
        requireNonEmpty(archiveFiles, "archiveFiles");
        for (Path archive : archiveFiles) {
            extract(archive, targetDir.resolve(baseName(archive.getFileName().toString())));
        }
    }

    /**
     * 按分组压缩。
     *
     * @param groups    ZIP 文件名与源文件集合映射
     * @param targetDir 目标目录
     * @return 生成的 ZIP 路径映射
     */
    public static Map<String, Path> zipByGroup(Map<String, Collection<Path>> groups, Path targetDir) {
        requireNonEmpty(groups.entrySet(), "groups");
        requireParent(targetDir.resolve("placeholder"), true);
        Map<String, Path> result = new LinkedHashMap<>();
        groups.forEach((name, paths) -> {
            Path zipPath = targetDir.resolve(sanitizeFileName(name) + ".zip");
            zip(paths, zipPath);
            result.put(name, zipPath);
        });
        return result;
    }

    /**
     * 合并多个 ZIP。
     *
     * @param zipFiles  ZIP 文件集合
     * @param targetZip 目标 ZIP
     */
    public static void mergeZips(Collection<Path> zipFiles, Path targetZip) {
        requireNonEmpty(zipFiles, "zipFiles");
        requireParent(targetZip, true);
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(targetZip))) {
            Set<String> names = new HashSet<>();
            for (Path zip : zipFiles) {
                try (java.util.zip.ZipFile zipFile = new java.util.zip.ZipFile(requireRegularFile(zip, "zip").toFile())) {
                    var enumeration = zipFile.entries();
                    while (enumeration.hasMoreElements()) {
                        ZipEntry entry = enumeration.nextElement();
                        if (entry.isDirectory() || !names.add(entry.getName())) {
                            continue;
                        }
                        out.putNextEntry(new ZipEntry(entry.getName()));
                        try (InputStream in = zipFile.getInputStream(entry)) {
                            in.transferTo(out);
                        }
                        out.closeEntry();
                    }
                }
            }
        } catch (IOException e) {
            throw new ZipUtilException("合并 ZIP 失败: " + targetZip, e);
        }
    }

    /**
     * 按大小拆分源目录为多个 ZIP。
     *
     * @param source    源文件或目录
     * @param targetDir 目标目录
     * @param maxSize   每个 ZIP 最大源文件字节数
     * @return 生成的 ZIP 路径列表
     */
    public static List<Path> splitBySize(Path source, Path targetDir, long maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize 必须大于 0");
        }
        List<Path> files = listRegularFiles(source);
        List<Path> result = new ArrayList<>();
        List<Path> group = new ArrayList<>();
        long current = 0L;
        int index = 1;
        for (Path file : files) {
            long size = sizeOrZero(file);
            if (!group.isEmpty() && current + size > maxSize) {
                Path zip = targetDir.resolve("part-" + index++ + ".zip");
                zip(group, zip);
                result.add(zip);
                group.clear();
                current = 0L;
            }
            group.add(file);
            current += size;
        }
        if (!group.isEmpty()) {
            Path zip = targetDir.resolve("part-" + index + ".zip");
            zip(group, zip);
            result.add(zip);
        }
        return result;
    }

    /**
     * 归档指定时间之前的旧文件。
     *
     * @param dir         源目录
     * @param olderThan   老化时间
     * @param archivePath 目标压缩包路径
     */
    public static void archiveOldFiles(Path dir, Duration olderThan, Path archivePath) {
        requireDirectory(dir, "dir");
        Objects.requireNonNull(olderThan, "olderThan 不能为空");
        Instant threshold = Instant.now().minus(olderThan);
        List<Path> files = listRegularFiles(dir).stream().filter(path -> {
            try {
                return Files.getLastModifiedTime(path).toInstant().isBefore(threshold);
            } catch (IOException e) {
                return false;
            }
        }).toList();
        if (files.isEmpty()) {
            throw new ZipUtilException("没有可归档的旧文件: " + dir);
        }
        zip(files, archivePath);
    }

    private static void doZipToStream(Collection<Path> sources, OutputStream out, ZipOptions options) {
        ProgressListener listener = options.progressListener() == null ? ProgressListener.noop() : options.progressListener();
        long totalBytes = estimateWorkloadSafe(sources);
        listener.onStart("zip", totalBytes, listAllEntries(sources).size());
        try (ZipOutputStream zipOut = new ZipOutputStream(new BufferedOutputStream(out), options.charset())) {
            if (options.comment() != null) {
                zipOut.setComment(options.comment());
            }
            if (options.compressionLevel() >= 0) {
                zipOut.setLevel(options.compressionLevel());
            }
            for (Path source : sources) {
                Path actual = requirePath(source, "source");
                Path base = options.includeRootDir() ? actual.getParent() : actual;
                writeZipEntries(actual, base, zipOut, options, listener, totalBytes, new long[]{0});
            }
            listener.onComplete("zip", totalBytes);
        } catch (IOException e) {
            listener.onError("zip", e);
            throw new ZipUtilException("流式 ZIP 压缩失败", e);
        }
    }

    private static void writeZipEntries(Path source, Path base, ZipOutputStream zipOut, ZipOptions options,
                                        ProgressListener listener, long totalBytes, long[] processed) throws IOException {
        if (options.entryFilter() != null && !options.entryFilter().test(source)) {
            return;
        }
        if (Files.isDirectory(source)) {
            List<Path> children = listChildren(source);
            if (children.isEmpty() && options.includeEmptyDir()) {
                String dirName = toEntryName(base, source, true);
                String mapped = options.entryNameMapper().map(source, dirName);
                zipOut.putNextEntry(new ZipEntry(validateEntryName(mapped)));
                zipOut.closeEntry();
            }
            for (Path child : children) {
                writeZipEntries(child, base, zipOut, options, listener, totalBytes, processed);
            }
            return;
        }
        String entryName = options.entryNameMapper().map(source, toEntryName(base, source, false));
        validateEntryName(entryName);
        listener.onEntryStart(entryName);
        ZipEntry entry = new ZipEntry(entryName);
        entry.setLastModifiedTime(Files.getLastModifiedTime(source));
        zipOut.putNextEntry(entry);
        try (InputStream in = Files.newInputStream(source)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int len;
            while ((len = in.read(buffer)) != -1) {
                zipOut.write(buffer, 0, len);
                processed[0] += len;
                listener.onProgress(entryName, processed[0], totalBytes, percent(processed[0], totalBytes));
            }
        }
        zipOut.closeEntry();
        listener.onEntryComplete(entryName);
    }

    private static void safeUnzipInternal(Path zipPath, Path targetDir, UnzipOptions options) {
        requireParent(targetDir.resolve("placeholder"), true);
        ProgressListener listener = options.progressListener() == null ? ProgressListener.noop() : options.progressListener();
        List<ArchiveEntryInfo> entries = listEntries(zipPath);
        long totalBytes = entries.stream().mapToLong(e -> Math.max(e.uncompressedSize(), 0)).sum();
        listener.onStart("unzip", totalBytes, entries.size());
        try (ZipInputStream zipIn = new ZipInputStream(new BufferedInputStream(Files.newInputStream(zipPath)), options.charset())) {
            ZipEntry entry;
            long processed = 0L;
            while ((entry = zipIn.getNextEntry()) != null) {
                String entryName = options.stripRootDir() ? removeRoot(entry.getName()) : entry.getName();
                if (entryName.isBlank()) {
                    continue;
                }
                ArchiveEntryInfo info = fromZipEntry(entry);
                if (options.entryFilter() != null && !options.entryFilter().test(info)) {
                    continue;
                }
                Path target = safeResolve(targetDir, entryName);
                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                    continue;
                }
                Path resolved = resolveConflict(target, options.conflictStrategy());
                if (resolved == null) {
                    continue;
                }
                listener.onEntryStart(entryName);
                requireParent(resolved, true);
                try (OutputStream out = Files.newOutputStream(resolved)) {
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int len;
                    while ((len = zipIn.read(buffer)) != -1) {
                        out.write(buffer, 0, len);
                        processed += len;
                        listener.onProgress(entryName, processed, totalBytes, percent(processed, totalBytes));
                    }
                }
                if (options.preserveLastModifiedTime() && entry.getLastModifiedTime() != null) {
                    Files.setLastModifiedTime(resolved, entry.getLastModifiedTime());
                }
                listener.onEntryComplete(entryName);
            }
            listener.onComplete("unzip", totalBytes);
        } catch (IOException e) {
            listener.onError("unzip", e);
            throw new ZipUtilException("安全解压 ZIP 失败: " + zipPath, e);
        }
    }

    private static void unzipWithZip4jPassword(Path zipPath, Path targetDir, char[] password) {
        requireRegularFile(zipPath, "zipPath");
        requireParent(targetDir.resolve("placeholder"), true);
        try (ZipFile zipFile = password == null ? new ZipFile(zipPath.toFile()) : new ZipFile(zipPath.toFile(), password)) {
            zipFile.extractAll(targetDir.toString());
        } catch (IOException e) {
            throw new ZipUtilException("Zip4j 解压失败: " + zipPath, e);
        }
    }

    private static void zipWithZip4jEncryption(Collection<Path> sources, Path zipPath, char[] password, ZipEncryptionMethod method, long splitSize) {
        requireNonEmpty(sources, "sources");
        requirePassword(password);
        requireParent(zipPath, true);
        ZipEncryptionMethod actualMethod = method == null ? ZipEncryptionMethod.AES : method;
        List<Path> list = sources.stream().map(path -> requirePath(path, "source")).toList();
        try (ZipFile zipFile = new ZipFile(zipPath.toFile(), password)) {
            ZipParameters parameters = defaultZip4jParameters();
            parameters.setEncryptFiles(true);
            if (actualMethod == ZipEncryptionMethod.AES) {
                parameters.setEncryptionMethod(net.lingala.zip4j.model.enums.EncryptionMethod.AES);
                parameters.setAesKeyStrength(AesKeyStrength.KEY_STRENGTH_256);
            } else {
                parameters.setEncryptionMethod(net.lingala.zip4j.model.enums.EncryptionMethod.ZIP_STANDARD);
            }
            if (splitSize >= MIN_SPLIT_SIZE && list.size() == 1 && Files.isDirectory(list.getFirst())) {
                zipFile.createSplitZipFileFromFolder(list.getFirst().toFile(), parameters, true, splitSize);
                return;
            }
            if (splitSize >= MIN_SPLIT_SIZE && list.stream().allMatch(Files::isRegularFile)) {
                zipFile.createSplitZipFile(list.stream().map(Path::toFile).toList(), parameters, true, splitSize);
                return;
            }
            for (Path source : list) {
                if (Files.isDirectory(source)) {
                    zipFile.addFolder(source.toFile(), parameters);
                } else {
                    zipFile.addFile(source.toFile(), parameters);
                }
            }
        } catch (IOException e) {
            throw new ZipUtilException("Zip4j 加密压缩失败: " + zipPath, e);
        }
    }

    private static void zipWithZip4jEncryption(Path source, Path zipPath, char[] password, ZipEncryptionMethod method, long splitSize) {
        requirePath(source, "source");
        requirePassword(password);
        requireParent(zipPath, true);
        try (ZipFile zipFile = new ZipFile(zipPath.toFile(), password)) {
            ZipParameters parameters = defaultZip4jParameters();
            parameters.setEncryptFiles(true);
            ZipEncryptionMethod actualMethod = method == null ? ZipEncryptionMethod.AES : method;
            if (actualMethod == ZipEncryptionMethod.AES) {
                parameters.setEncryptionMethod(net.lingala.zip4j.model.enums.EncryptionMethod.AES);
                parameters.setAesKeyStrength(AesKeyStrength.KEY_STRENGTH_256);
            } else {
                parameters.setEncryptionMethod(net.lingala.zip4j.model.enums.EncryptionMethod.ZIP_STANDARD);
            }
            if (splitSize >= MIN_SPLIT_SIZE) {
                if (Files.isDirectory(source)) {
                    zipFile.createSplitZipFileFromFolder(source.toFile(), parameters, true, splitSize);
                } else {
                    zipFile.createSplitZipFile(List.of(source.toFile()), parameters, true, splitSize);
                }
            } else if (Files.isDirectory(source)) {
                zipFile.addFolder(source.toFile(), parameters);
            } else {
                zipFile.addFile(source.toFile(), parameters);
            }
        } catch (IOException e) {
            throw new ZipUtilException("Zip4j 加密压缩失败: " + zipPath, e);
        }
    }

    private static ZipParameters defaultZip4jParameters() {
        ZipParameters parameters = new ZipParameters();
        parameters.setCompressionMethod(CompressionMethod.DEFLATE);
        parameters.setCompressionLevel(CompressionLevel.NORMAL);
        return parameters;
    }

    private static void addStreamEntry(Path zipPath, String entryName, InputStream in) throws IOException {
        requireParent(zipPath, true);
        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            ZipParameters parameters = defaultZip4jParameters();
            parameters.setFileNameInZip(validateEntryName(entryName));
            zipFile.addStream(in, parameters);
        }
    }

    private static List<ArchiveEntryInfo> listZipEntries(Path zipPath) throws IOException {
        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            List<ArchiveEntryInfo> result = new ArrayList<>();
            for (FileHeader header : zipFile.getFileHeaders()) {
                FileTime time = header.getLastModifiedTimeEpoch() > 0 ? FileTime.fromMillis(header.getLastModifiedTimeEpoch()) : null;
                result.add(new ArchiveEntryInfo(header.getFileName(), Path.of(header.getFileName()).getFileName().toString(), header.isDirectory(),
                        header.getCompressedSize(), header.getUncompressedSize(), time, header.getFileComment(), header.isEncrypted(), 0, header.getCrc()));
            }
            return result;
        }
    }

    private static List<ArchiveEntryInfo> listTarEntries(InputStream rawInput) throws IOException {
        try (InputStream in = rawInput; TarArchiveInputStream tarIn = new TarArchiveInputStream(new BufferedInputStream(in))) {
            List<ArchiveEntryInfo> result = new ArrayList<>();
            TarArchiveEntry entry;
            while ((entry = tarIn.getNextEntry()) != null) {
                FileTime time = entry.getLastModifiedDate() == null ? null : FileTime.from(entry.getLastModifiedDate().toInstant());
                result.add(new ArchiveEntryInfo(entry.getName(), Path.of(entry.getName()).getFileName().toString(), entry.isDirectory(), -1L, entry.getSize(), time, null, false, entry.getMode(), 0));
            }
            return result;
        }
    }

    private static List<ArchiveEntryInfo> listSevenZEntries(Path sevenZPath) throws IOException {
        try (SevenZFile sevenZFile = new SevenZFile(sevenZPath.toFile())) {
            List<ArchiveEntryInfo> result = new ArrayList<>();
            SevenZArchiveEntry entry;
            while ((entry = sevenZFile.getNextEntry()) != null) {
                FileTime time = entry.getLastModifiedDate() == null ? null : FileTime.from(entry.getLastModifiedDate().toInstant());
                result.add(new ArchiveEntryInfo(entry.getName(), Path.of(entry.getName()).getFileName().toString(), entry.isDirectory(), -1L, entry.getSize(), time, null, false, 0, entry.getHasCrc() ? entry.getCrcValue() : 0));
            }
            return result;
        }
    }

    private static ArchiveEntryInfo singleCompressedEntry(Path path, ArchiveFormat format) {
        return new ArchiveEntryInfo(stripKnownExtension(path.getFileName().toString()), stripKnownExtension(path.getFileName().toString()), false,
                sizeOrZero(path), -1L, lastModifiedTime(path), null, false, 0, 0);
    }

    private static void untar(Path tarPath, Path targetDir, ExtractOptions options) {
        requireRegularFile(tarPath, "tarPath");
        try (InputStream in = Files.newInputStream(tarPath)) {
            extractTarStream(in, targetDir, options);
        } catch (IOException e) {
            throw new ZipUtilException("TAR 解包失败: " + tarPath, e);
        }
    }

    private static void tarWithCompressor(Path source, Path archivePath, String compressor) {
        requirePath(source, "source");
        requireParent(archivePath, true);
        try (OutputStream fos = Files.newOutputStream(archivePath);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             CompressorOutputStream<?> cos = CompressorStreamFactory.getSingleton().createCompressorOutputStream(compressor, bos);
             TarArchiveOutputStream tarOut = new TarArchiveOutputStream(cos)) {
            tarOut.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
            writeTarEntries(source, source.getParent(), tarOut, ZipOptions.defaults());
        } catch (IOException | NoClassDefFoundError e) {
            throw new ZipUtilException("TAR 压缩打包失败: " + archivePath, e);
        }
    }

    private static void unTarWithCompressor(Path archivePath, Path targetDir, String compressor) {
        unTarWithCompressor(archivePath, targetDir, compressor, ExtractOptions.defaults());
    }

    private static void unTarWithCompressor(Path archivePath, Path targetDir, String compressor, ExtractOptions options) {
        requireRegularFile(archivePath, "archivePath");
        ExtractOptions actual = options == null ? ExtractOptions.defaults() : options;
        try (InputStream fis = Files.newInputStream(archivePath);
             BufferedInputStream bis = new BufferedInputStream(fis);
             CompressorInputStream cis = CompressorStreamFactory.getSingleton().createCompressorInputStream(compressor, bis)) {
            extractTarStream(cis, targetDir, actual);
        } catch (IOException | NoClassDefFoundError e) {
            throw new ZipUtilException("TAR 压缩包解包失败: " + archivePath, e);
        }
    }

    private static InputStream newCompressorInput(Path path, String compressor) throws IOException {
        try {
            return CompressorStreamFactory.getSingleton().createCompressorInputStream(compressor, new BufferedInputStream(Files.newInputStream(path)));
        } catch (CompressorException e) {
            throw new IOException(e);
        }
    }

    private static void writeTarEntries(Path source, Path base, TarArchiveOutputStream tarOut, ZipOptions options) throws IOException {
        if (options.entryFilter() != null && !options.entryFilter().test(source)) {
            return;
        }
        String entryName = toEntryName(base, source, Files.isDirectory(source));
        TarArchiveEntry entry = new TarArchiveEntry(source.toFile(), entryName);
        tarOut.putArchiveEntry(entry);
        if (Files.isRegularFile(source)) {
            Files.copy(source, tarOut);
        }
        tarOut.closeArchiveEntry();
        if (Files.isDirectory(source)) {
            for (Path child : listChildren(source)) {
                writeTarEntries(child, base, tarOut, options);
            }
        }
    }

    private static void extractTarStream(InputStream in, Path targetDir, ExtractOptions options) throws IOException {
        requireParent(targetDir.resolve("placeholder"), true);
        try (TarArchiveInputStream tarIn = new TarArchiveInputStream(new BufferedInputStream(in))) {
            TarArchiveEntry entry;
            while ((entry = tarIn.getNextEntry()) != null) {
                String entryName = options.stripRootDir() ? removeRoot(entry.getName()) : entry.getName();
                if (entryName.isBlank()) {
                    continue;
                }
                Path target = safeResolve(targetDir, entryName);
                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                    continue;
                }
                Path resolved = resolveConflict(target, options.conflictStrategy());
                if (resolved == null) {
                    continue;
                }
                requireParent(resolved, true);
                Files.copy(tarIn, resolved, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private static void compressSingle(Path source, Path targetPath, String compressor) {
        requireRegularFile(source, "source");
        requireParent(targetPath, true);
        try (OutputStream fos = Files.newOutputStream(targetPath);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             CompressorOutputStream<?> cos = CompressorStreamFactory.getSingleton().createCompressorOutputStream(compressor, bos);
             InputStream in = Files.newInputStream(source)) {
            in.transferTo(cos);
        } catch (IOException | NoClassDefFoundError e) {
            throw new ZipUtilException("单文件压缩失败: " + targetPath, e);
        }
    }

    private static void decompressSingle(Path source, Path targetPath, String compressor) {
        requireRegularFile(source, "source");
        requireParent(targetPath, true);
        try (InputStream fis = Files.newInputStream(source);
             BufferedInputStream bis = new BufferedInputStream(fis);
             CompressorInputStream cis = CompressorStreamFactory.getSingleton().createCompressorInputStream(compressor, bis)) {
            Files.copy(cis, targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException | NoClassDefFoundError e) {
            throw new ZipUtilException("单文件解压失败: " + source, e);
        }
    }

    private static void writeSevenZEntries(Path source, Path base, SevenZOutputFile out, ZipOptions options) throws IOException {
        if (options.entryFilter() != null && !options.entryFilter().test(source)) {
            return;
        }
        String entryName = toEntryName(base, source, Files.isDirectory(source));
        SevenZArchiveEntry entry = out.createArchiveEntry(source.toFile(), entryName);
        out.putArchiveEntry(entry);
        if (Files.isRegularFile(source)) {
            try (InputStream in = Files.newInputStream(source)) {
                byte[] buffer = new byte[BUFFER_SIZE];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
            }
        }
        out.closeArchiveEntry();
        if (Files.isDirectory(source)) {
            for (Path child : listChildren(source)) {
                writeSevenZEntries(child, base, out, options);
            }
        }
    }

    private static void copySevenZEntry(SevenZFile sevenZFile, OutputStream out) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int len;
        while ((len = sevenZFile.read(buffer)) > 0) {
            out.write(buffer, 0, len);
        }
    }

    private static List<ArchiveEntryInfo> listEntriesIfSupported(Path archivePath) {
        try {
            return listEntries(archivePath);
        } catch (RuntimeException e) {
            return List.of();
        }
    }

    private static void validateEntryBySecurity(ArchiveEntryInfo entry, SecurityOptions security) {
        String name = entry.name();
        if (!security.allowPathTraversal() && isZipSlipEntry(name)) {
            throw new ZipUtilException("条目存在路径穿越风险: " + name);
        }
        if (entry.uncompressedSize() > security.maxEntrySize()) {
            throw new ZipUtilException("单个条目大小超过限制: " + name);
        }
        String ext = extension(name);
        if (!security.allowedExtensions().isEmpty() && !security.allowedExtensions().contains(ext)) {
            throw new ZipUtilException("条目扩展名不在允许范围: " + name);
        }
        if (security.blockedExtensions().contains(ext)) {
            throw new ZipUtilException("条目扩展名被禁止: " + name);
        }
    }

    private static Path requirePath(Path path, String name) {
        requirePathObject(path, name);
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
            throw new ZipUtilException(name + " 不存在: " + path);
        }
        return path;
    }

    private static Path requirePathObject(Path path, String name) {
        if (path == null) {
            throw new IllegalArgumentException(name + " 不能为空");
        }
        return path;
    }

    private static Path requireRegularFile(Path path, String name) {
        requirePath(path, name);
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            throw new ZipUtilException(name + " 不是文件: " + path);
        }
        return path;
    }

    private static void requireDirectory(Path path, String name) {
        requirePath(path, name);
        if (!Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
            throw new ZipUtilException(name + " 不是目录: " + path);
        }
    }

    private static void requireParent(Path path, boolean create) {
        requirePathObject(path, "path");
        Path parent = path.toAbsolutePath().normalize().getParent();
        if (parent == null) {
            return;
        }
        try {
            if (create) {
                Files.createDirectories(parent);
            } else if (!Files.isDirectory(parent)) {
                throw new ZipUtilException("父目录不存在: " + parent);
            }
        } catch (IOException e) {
            throw new ZipUtilException("创建父目录失败: " + parent, e);
        }
    }

    private static <T> void requireNonEmpty(Collection<T> collection, String name) {
        if (collection == null || collection.isEmpty()) {
            throw new IllegalArgumentException(name + " 不能为空");
        }
    }

    private static void requirePassword(char[] password) {
        if (password == null || password.length == 0) {
            throw new IllegalArgumentException("password 不能为空");
        }
    }

    private static OutputStream openOutput(Path path, boolean createParent) {
        try {
            if (createParent) {
                requireParent(path, true);
            }
            return Files.newOutputStream(path);
        } catch (IOException e) {
            throw new ZipUtilException("打开输出流失败: " + path, e);
        }
    }

    private static String toEntryName(Path base, Path path, boolean directory) {
        Path relative = base == null ? path.getFileName() : base.relativize(path);
        String name = relative.toString().replace('\\', '/');
        return directory && !name.endsWith("/") ? name + "/" : name;
    }

    private static List<Path> listChildren(Path dir) throws IOException {
        try (Stream<Path> stream = Files.list(dir)) {
            return stream.sorted().toList();
        }
    }

    private static List<Path> listRegularFiles(Path source) {
        requirePath(source, "source");
        try (Stream<Path> stream = Files.walk(source)) {
            return stream.filter(Files::isRegularFile).sorted().toList();
        } catch (IOException e) {
            throw new ZipUtilException("读取文件列表失败: " + source, e);
        }
    }

    private static List<Path> listAllEntries(Collection<Path> sources) {
        List<Path> result = new ArrayList<>();
        for (Path source : sources) {
            try (Stream<Path> stream = Files.walk(source)) {
                result.addAll(stream.toList());
            } catch (IOException e) {
                throw new ZipUtilException("读取文件列表失败: " + source, e);
            }
        }
        return result;
    }

    private static long totalFileSize(Path path) {
        requirePath(path, "path");
        if (Files.isRegularFile(path)) {
            return sizeOrZero(path);
        }
        try (Stream<Path> stream = Files.walk(path)) {
            return stream.filter(Files::isRegularFile).mapToLong(ZipUtil::sizeOrZero).sum();
        } catch (IOException e) {
            throw new ZipUtilException("统计文件大小失败: " + path, e);
        }
    }

    private static long estimateWorkloadSafe(Collection<Path> sources) {
        try {
            return estimateWorkload(sources);
        } catch (RuntimeException e) {
            return 0L;
        }
    }

    private static long sizeOrZero(Path path) {
        try {
            return Files.size(path);
        } catch (IOException e) {
            return 0L;
        }
    }

    private static FileTime lastModifiedTime(Path path) {
        try {
            return Files.getLastModifiedTime(path);
        } catch (IOException e) {
            return null;
        }
    }

    private static int percent(long processed, long total) {
        if (total <= 0) {
            return 100;
        }
        return (int) Math.min(100, processed * 100 / total);
    }

    private static ArchiveEntryInfo fromZipEntry(ZipEntry entry) {
        return new ArchiveEntryInfo(entry.getName(), Path.of(entry.getName()).getFileName().toString(), entry.isDirectory(), entry.getCompressedSize(), entry.getSize(), entry.getLastModifiedTime(), entry.getComment(), false, 0, entry.getCrc());
    }

    private static String removeRoot(String name) {
        String normalized = sanitizeEntryName(name);
        int index = normalized.indexOf('/');
        return index < 0 ? normalized : normalized.substring(index + 1);
    }

    private static boolean hasChildren(Path path) {
        try (Stream<Path> stream = Files.list(path)) {
            return stream.findAny().isPresent();
        } catch (IOException e) {
            return false;
        }
    }

    private static Set<String> normalizeExtensions(Set<String> extensions) {
        if (extensions == null || extensions.isEmpty()) {
            return Set.of();
        }
        return extensions.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.startsWith(".") ? s.toLowerCase(Locale.ROOT) : "." + s.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }

    private static String extension(String name) {
        if (name == null) {
            return "";
        }
        String clean = name.toLowerCase(Locale.ROOT);
        int slash = clean.lastIndexOf('/');
        String fileName = slash >= 0 ? clean.substring(slash + 1) : clean;
        int dot = fileName.lastIndexOf('.');
        return dot >= 0 ? fileName.substring(dot) : "";
    }

    private static String stripKnownExtension(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        for (String ext : List.of(".tar.gz", ".tar.bz2", ".tar.xz", ".tgz", ".tbz2", ".txz", ".zip", ".jar", ".tar", ".gz", ".bz2", ".xz", ".7z")) {
            if (lower.endsWith(ext)) {
                return name.substring(0, name.length() - ext.length());
            }
        }
        return name;
    }

    private static String baseName(String name) {
        return stripKnownExtension(name);
    }

    private static String suffix(String name) {
        int index = name.lastIndexOf('.');
        return index >= 0 ? name.substring(index) : "";
    }

    private static ZipEntry copyZipEntryMetadata(ZipEntry source) {
        ZipEntry target = new ZipEntry(source.getName());
        target.setComment(source.getComment());
        if (source.getTime() >= 0) {
            target.setTime(source.getTime());
        }
        if (source.getLastModifiedTime() != null) {
            target.setLastModifiedTime(source.getLastModifiedTime());
        }
        if (source.getCreationTime() != null) {
            target.setCreationTime(source.getCreationTime());
        }
        if (source.getLastAccessTime() != null) {
            target.setLastAccessTime(source.getLastAccessTime());
        }
        return target;
    }

    private static String digest(Path file, String algorithm) {
        requireRegularFile(file, "file");
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            try (DigestInputStream in = new DigestInputStream(Files.newInputStream(file), digest)) {
                in.transferTo(OutputStream.nullOutputStream());
            }
            StringBuilder sb = new StringBuilder();
            for (byte b : digest.digest()) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new ZipUtilException("计算文件摘要失败: " + file, e);
        }
    }

    /**
     * 压缩包格式。
     */
    public enum ArchiveFormat {
        ZIP, JAR, TAR, TAR_GZ, TGZ, TAR_BZ2, TBZ2, TAR_XZ, TXZ, GZIP, BZIP2, XZ, SEVEN_Z, UNKNOWN
    }

    /**
     * 压缩流格式。
     */
    public enum CompressorFormat {
        NONE, GZIP, BZIP2, XZ, DEFLATE
    }

    /**
     * 文件冲突策略。
     */
    public enum ConflictStrategy {
        OVERWRITE, SKIP, RENAME, BACKUP, FAIL
    }

    /**
     * ZIP 加密方式。
     */
    public enum ZipEncryptionMethod {
        AES, STANDARD
    }

    /**
     * 条目名称映射器。
     */
    @FunctionalInterface
    public interface EntryNameMapper {
        /**
         * 映射压缩包内条目名称。
         *
         * @param source    源文件路径
         * @param entryName 默认条目名称
         * @return 新条目名称
         */
        String map(Path source, String entryName);

        /**
         * 返回默认映射器。
         *
         * @return 默认映射器
         */
        static EntryNameMapper identity() {
            return (source, entryName) -> entryName;
        }
    }

    /**
     * 输入流供应器。
     */
    @FunctionalInterface
    public interface InputStreamSupplier {
        /**
         * 打开输入流。
         *
         * @return 输入流
         * @throws IOException 打开失败
         */
        InputStream open() throws IOException;
    }

    /**
     * 进度监听器。
     */
    public interface ProgressListener {
        /**
         * 任务开始回调。
         *
         * @param operation    操作名称
         * @param totalBytes   总字节数
         * @param totalEntries 总条目数
         */
        default void onStart(String operation, long totalBytes, int totalEntries) {
        }

        /**
         * 条目开始回调。
         *
         * @param entryName 条目名称
         */
        default void onEntryStart(String entryName) {
        }

        /**
         * 进度变化回调。
         *
         * @param entryName      当前条目名称
         * @param processedBytes 已处理字节数
         * @param totalBytes     总字节数
         * @param percent        百分比
         */
        default void onProgress(String entryName, long processedBytes, long totalBytes, int percent) {
        }

        /**
         * 条目完成回调。
         *
         * @param entryName 条目名称
         */
        default void onEntryComplete(String entryName) {
        }

        /**
         * 任务完成回调。
         *
         * @param operation  操作名称
         * @param totalBytes 总字节数
         */
        default void onComplete(String operation, long totalBytes) {
        }

        /**
         * 任务异常回调。
         *
         * @param operation 操作名称
         * @param throwable 异常
         */
        default void onError(String operation, Throwable throwable) {
        }

        /**
         * 返回空监听器。
         *
         * @return 空监听器
         */
        static ProgressListener noop() {
            return new ProgressListener() {
            };
        }
    }

    /**
     * ZIP 压缩选项。
     *
     * @param charset          文件名编码
     * @param compressionLevel 压缩级别，-1 表示默认
     * @param includeRootDir   是否包含根目录
     * @param includeEmptyDir  是否包含空目录
     * @param overwrite        是否覆盖目标 ZIP
     * @param comment          ZIP 注释
     * @param password         密码
     * @param encryptionMethod 加密方式
     * @param splitSize        分卷大小
     * @param entryFilter      文件过滤器
     * @param entryNameMapper  条目名映射器
     * @param progressListener 进度监听器
     */
    public record ZipOptions(Charset charset, int compressionLevel, boolean includeRootDir, boolean includeEmptyDir,
                             boolean overwrite, String comment, char[] password, ZipEncryptionMethod encryptionMethod,
                             long splitSize, Predicate<Path> entryFilter, EntryNameMapper entryNameMapper,
                             ProgressListener progressListener) {
        /**
         * 创建 ZIP 压缩选项。
         */
        public ZipOptions {
            charset = normalizeEncoding(charset);
            entryNameMapper = entryNameMapper == null ? EntryNameMapper.identity() : entryNameMapper;
            password = password == null ? null : password.clone();
        }

        /**
         * 默认 ZIP 压缩选项。
         *
         * @return 默认选项
         */
        public static ZipOptions defaults() {
            return new ZipOptions(StandardCharsets.UTF_8, -1, true, true, true, null, null, ZipEncryptionMethod.AES, 0L, null, EntryNameMapper.identity(), null);
        }

        /**
         * 设置文件名编码。
         *
         * @param charset 文件名编码
         * @return 新选项
         */
        public ZipOptions withCharset(Charset charset) {
            return new ZipOptions(charset, compressionLevel, includeRootDir, includeEmptyDir, overwrite, comment, password, encryptionMethod, splitSize, entryFilter, entryNameMapper, progressListener);
        }

        /**
         * 设置文件过滤器。
         *
         * @param filter 文件过滤器
         * @return 新选项
         */
        public ZipOptions withEntryFilter(Predicate<Path> filter) {
            return new ZipOptions(charset, compressionLevel, includeRootDir, includeEmptyDir, overwrite, comment, password, encryptionMethod, splitSize, filter, entryNameMapper, progressListener);
        }

        /**
         * 设置条目名映射器。
         *
         * @param mapper 条目名映射器
         * @return 新选项
         */
        public ZipOptions withEntryNameMapper(EntryNameMapper mapper) {
            return new ZipOptions(charset, compressionLevel, includeRootDir, includeEmptyDir, overwrite, comment, password, encryptionMethod, splitSize, entryFilter, mapper, progressListener);
        }

        /**
         * 设置进度监听器。
         *
         * @param listener 进度监听器
         * @return 新选项
         */
        public ZipOptions withProgressListener(ProgressListener listener) {
            return new ZipOptions(charset, compressionLevel, includeRootDir, includeEmptyDir, overwrite, comment, password, encryptionMethod, splitSize, entryFilter, entryNameMapper, listener);
        }
    }

    /**
     * ZIP 解压选项。
     *
     * @param charset                  文件名编码
     * @param password                 密码
     * @param conflictStrategy         冲突策略
     * @param entryFilter              条目过滤器
     * @param securityOptions          安全选项
     * @param stripRootDir             是否去掉根目录
     * @param preserveLastModifiedTime 是否保留修改时间
     * @param progressListener         进度监听器
     */
    public record UnzipOptions(Charset charset, char[] password, ConflictStrategy conflictStrategy,
                               Predicate<ArchiveEntryInfo> entryFilter, SecurityOptions securityOptions,
                               boolean stripRootDir, boolean preserveLastModifiedTime,
                               ProgressListener progressListener) {
        /**
         * 创建 ZIP 解压选项。
         */
        public UnzipOptions {
            charset = normalizeEncoding(charset);
            password = password == null ? null : password.clone();
            conflictStrategy = conflictStrategy == null ? ConflictStrategy.FAIL : conflictStrategy;
            securityOptions = securityOptions == null ? SecurityOptions.defaults() : securityOptions;
        }

        /**
         * 默认 ZIP 解压选项。
         *
         * @return 默认选项
         */
        public static UnzipOptions defaults() {
            return new UnzipOptions(StandardCharsets.UTF_8, null, ConflictStrategy.FAIL, null, SecurityOptions.defaults(), false, true, null);
        }

        /**
         * 设置文件名编码。
         *
         * @param charset 文件名编码
         * @return 新选项
         */
        public UnzipOptions withCharset(Charset charset) {
            return new UnzipOptions(charset, password, conflictStrategy, entryFilter, securityOptions, stripRootDir, preserveLastModifiedTime, progressListener);
        }

        /**
         * 设置冲突策略。
         *
         * @param strategy 冲突策略
         * @return 新选项
         */
        public UnzipOptions withConflictStrategy(ConflictStrategy strategy) {
            return new UnzipOptions(charset, password, strategy, entryFilter, securityOptions, stripRootDir, preserveLastModifiedTime, progressListener);
        }

        /**
         * 设置条目过滤器。
         *
         * @param filter 条目过滤器
         * @return 新选项
         */
        public UnzipOptions withEntryFilter(Predicate<ArchiveEntryInfo> filter) {
            return new UnzipOptions(charset, password, conflictStrategy, filter, securityOptions, stripRootDir, preserveLastModifiedTime, progressListener);
        }

        /**
         * 设置进度监听器。
         *
         * @param listener 进度监听器
         * @return 新选项
         */
        public UnzipOptions withProgressListener(ProgressListener listener) {
            return new UnzipOptions(charset, password, conflictStrategy, entryFilter, securityOptions, stripRootDir, preserveLastModifiedTime, listener);
        }
    }

    /**
     * 通用解包选项。
     *
     * @param format                   指定格式，UNKNOWN 表示自动识别
     * @param charset                  文件名编码
     * @param password                 密码
     * @param conflictStrategy         冲突策略
     * @param entryFilter              条目过滤器
     * @param securityOptions          安全选项
     * @param stripRootDir             是否去掉根目录
     * @param preserveLastModifiedTime 是否保留修改时间
     * @param progressListener         进度监听器
     */
    public record ExtractOptions(ArchiveFormat format, Charset charset, char[] password,
                                 ConflictStrategy conflictStrategy,
                                 Predicate<ArchiveEntryInfo> entryFilter, SecurityOptions securityOptions,
                                 boolean stripRootDir, boolean preserveLastModifiedTime,
                                 ProgressListener progressListener) {
        /**
         * 创建通用解包选项。
         */
        public ExtractOptions {
            format = format == null ? ArchiveFormat.UNKNOWN : format;
            charset = normalizeEncoding(charset);
            password = password == null ? null : password.clone();
            conflictStrategy = conflictStrategy == null ? ConflictStrategy.FAIL : conflictStrategy;
            securityOptions = securityOptions == null ? SecurityOptions.defaults() : securityOptions;
        }

        /**
         * 默认通用解包选项。
         *
         * @return 默认选项
         */
        public static ExtractOptions defaults() {
            return new ExtractOptions(ArchiveFormat.UNKNOWN, StandardCharsets.UTF_8, null, ConflictStrategy.FAIL, null, SecurityOptions.defaults(), false, true, null);
        }

        /**
         * 从 ZIP 解压选项转换。
         *
         * @param options ZIP 解压选项
         * @return 通用解包选项
         */
        public static ExtractOptions fromUnzipOptions(UnzipOptions options) {
            return new ExtractOptions(ArchiveFormat.ZIP, options.charset(), options.password(), options.conflictStrategy(), options.entryFilter(), options.securityOptions(), options.stripRootDir(), options.preserveLastModifiedTime(), options.progressListener());
        }

        /**
         * 转为 ZIP 解压选项。
         *
         * @return ZIP 解压选项
         */
        public UnzipOptions toUnzipOptions() {
            return new UnzipOptions(charset, password, conflictStrategy, entryFilter, securityOptions, stripRootDir, preserveLastModifiedTime, progressListener);
        }

        /**
         * 设置格式。
         *
         * @param format 格式
         * @return 新选项
         */
        public ExtractOptions withFormat(ArchiveFormat format) {
            return new ExtractOptions(format, charset, password, conflictStrategy, entryFilter, securityOptions, stripRootDir, preserveLastModifiedTime, progressListener);
        }

        /**
         * 设置条目过滤器。
         *
         * @param filter 条目过滤器
         * @return 新选项
         */
        public ExtractOptions withEntryFilter(Predicate<ArchiveEntryInfo> filter) {
            return new ExtractOptions(format, charset, password, conflictStrategy, filter, securityOptions, stripRootDir, preserveLastModifiedTime, progressListener);
        }

        /**
         * 设置冲突策略。
         *
         * @param strategy 冲突策略
         * @return 新选项
         */
        public ExtractOptions withConflictStrategy(ConflictStrategy strategy) {
            return new ExtractOptions(format, charset, password, strategy, entryFilter, securityOptions, stripRootDir, preserveLastModifiedTime, progressListener);
        }

        /**
         * 设置是否去掉根目录。
         *
         * @param stripRootDir 是否去掉根目录
         * @return 新选项
         */
        public ExtractOptions withStripRootDir(boolean stripRootDir) {
            return new ExtractOptions(format, charset, password, conflictStrategy, entryFilter, securityOptions, stripRootDir, preserveLastModifiedTime, progressListener);
        }

        /**
         * 设置进度监听器。
         *
         * @param listener 进度监听器
         * @return 新选项
         */
        public ExtractOptions withProgressListener(ProgressListener listener) {
            return new ExtractOptions(format, charset, password, conflictStrategy, entryFilter, securityOptions, stripRootDir, preserveLastModifiedTime, listener);
        }
    }

    /**
     * 安全选项。
     *
     * @param maxEntryCount       最大条目数
     * @param maxEntrySize        单条目最大字节数
     * @param maxTotalSize        总解压字节数
     * @param maxCompressionRatio 最大压缩比
     * @param allowedExtensions   允许扩展名
     * @param blockedExtensions   禁止扩展名
     * @param allowAbsolutePath   是否允许绝对路径
     * @param allowPathTraversal  是否允许路径穿越
     * @param allowSymlink        是否允许符号链接
     * @param overwriteExisting   是否允许覆盖已有文件
     */
    public record SecurityOptions(int maxEntryCount, long maxEntrySize, long maxTotalSize, double maxCompressionRatio,
                                  Set<String> allowedExtensions, Set<String> blockedExtensions,
                                  boolean allowAbsolutePath, boolean allowPathTraversal, boolean allowSymlink,
                                  boolean overwriteExisting) {
        /**
         * 创建安全选项。
         */
        public SecurityOptions {
            if (maxEntryCount <= 0 || maxEntrySize <= 0 || maxTotalSize <= 0) {
                throw new IllegalArgumentException("安全限制必须大于 0");
            }
            allowedExtensions = normalizeExtensions(allowedExtensions);
            blockedExtensions = normalizeExtensions(blockedExtensions);
        }

        /**
         * 默认安全选项。
         *
         * @return 默认安全选项
         */
        public static SecurityOptions defaults() {
            return new SecurityOptions(10_000, 512L * 1024 * 1024, 2L * 1024 * 1024 * 1024, 100.0D, Set.of(), Set.of(".exe", ".bat", ".cmd", ".sh"), false, false, false, false);
        }
    }

    /**
     * 压缩包条目信息。
     *
     * @param name             条目名称
     * @param fileName         文件名
     * @param directory        是否目录
     * @param compressedSize   压缩后大小
     * @param uncompressedSize 原始大小
     * @param lastModifiedTime 修改时间
     * @param comment          注释
     * @param encrypted        是否加密
     * @param unixMode         Unix 权限
     * @param crc              CRC 值
     */
    public record ArchiveEntryInfo(String name, String fileName, boolean directory, long compressedSize,
                                   long uncompressedSize, FileTime lastModifiedTime, String comment,
                                   boolean encrypted, int unixMode, long crc) {
    }

    /**
     * 压缩包摘要信息。
     *
     * @param path             压缩包路径
     * @param format           格式
     * @param entryCount       条目数
     * @param compressedSize   压缩包大小
     * @param uncompressedSize 解压后总大小
     * @param encrypted        是否加密
     */
    public record ArchiveInfo(Path path, ArchiveFormat format, int entryCount, long compressedSize,
                              long uncompressedSize, boolean encrypted) {
    }

    /**
     * 压缩包校验结果。
     *
     * @param valid                 是否通过校验
     * @param errors                错误列表
     * @param warnings              警告列表
     * @param entryCount            条目数
     * @param totalUncompressedSize 解压后总大小
     */
    public record ArchiveValidateResult(boolean valid, List<String> errors, List<String> warnings, int entryCount,
                                        long totalUncompressedSize) {
        /**
         * 创建压缩包校验结果。
         */
        public ArchiveValidateResult {
            errors = errors == null ? List.of() : List.copyOf(errors);
            warnings = warnings == null ? List.of() : List.copyOf(warnings);
        }
    }

    /**
     * 动态 ZIP 条目来源。
     *
     * @param entryName           条目名称
     * @param inputStreamSupplier 输入流供应器
     * @param size                字节大小
     * @param lastModifiedTime    修改时间
     */
    public record ZipEntrySource(String entryName, InputStreamSupplier inputStreamSupplier, long size,
                                 FileTime lastModifiedTime) {
        /**
         * 创建动态 ZIP 条目来源。
         */
        public ZipEntrySource {
            validateEntryName(entryName);
            Objects.requireNonNull(inputStreamSupplier, "inputStreamSupplier 不能为空");
        }
    }

    private static final class TaskControl {
        private volatile int percent;
        private volatile boolean cancelled;
        private volatile boolean paused;
    }
}