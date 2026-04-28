package local.ateng.java.awss3.utils;


import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Zip 工具类：支持将多个文件或目录压缩为 zip 文件或输出流
 *
 * @author Ateng
 * @since 2025-07-19
 */
public final class ZipUtil {

    /**
     * 缓冲区大小
     */
    private static final int BUFFER_SIZE = 8192;

    private ZipUtil() {
        // 工具类不能实例化
        throw new UnsupportedOperationException("工具类不可实例化");
    }

    /**
     * 将文件/目录压缩为 zip 文件
     *
     * @param source    要压缩的文件或目录
     * @param zipTarget 目标 zip 文件路径
     * @throws IOException 如果压缩过程中发生 I/O 错误，抛出异常
     */
    public static void zip(Path source, Path zipTarget) throws IOException {
        zip(Collections.singletonList(source), zipTarget);
    }

    /**
     * 将文件或目录压缩为zip文件，支持是否包含根目录
     *
     * @param source         文件或目录路径
     * @param zipTarget      目标zip文件路径
     * @param includeRootDir 是否包含根目录名
     * @throws IOException IO异常
     */
    public static void zip(Path source, Path zipTarget, boolean includeRootDir) throws IOException {
        Objects.requireNonNull(source, "source不能为空");
        Objects.requireNonNull(zipTarget, "zipTarget不能为空");

        if (!Files.exists(source)) {
            throw new IllegalArgumentException("待压缩路径不存在：" + source);
        }

        // 取目录下一层所有文件和目录（非递归）
        List<Path> children = Files.list(source).collect(Collectors.toList());
        if (includeRootDir) {
            zip(source, zipTarget);
        } else {
            zip(children, zipTarget);
        }

    }

    /**
     * 将文件或目录压缩为zip文件，支持是否包含根目录
     *
     * @param source         文件或目录路径
     * @param output         输出流
     * @param includeRootDir 是否包含根目录名
     * @throws IOException IO异常
     */
    public static void zip(Path source, OutputStream output, boolean includeRootDir) throws IOException {
        Objects.requireNonNull(source, "source不能为空");
        Objects.requireNonNull(output, "output不能为空");

        if (!Files.exists(source)) {
            throw new IllegalArgumentException("待压缩路径不存在：" + source);
        }

        // 取目录下一层所有文件和目录（非递归）
        List<Path> children = Files.list(source).collect(Collectors.toList());
        if (includeRootDir) {
            zip(Collections.singletonList(source), output);
        } else {
            zip(children, output);
        }

    }

    /**
     * 将文件或目录压缩为zip文件，支持是否包含根目录
     *
     * @param source         文件或目录路径
     * @param response       HttpServletResponse
     * @param zipFileName    是否包含根目录名
     * @param includeRootDir 下载文件名
     * @throws IOException IO异常
     */
    public static void zip(Path source, HttpServletResponse response, String zipFileName, boolean includeRootDir) throws IOException {
        Objects.requireNonNull(source, "source不能为空");
        Objects.requireNonNull(response, "HttpServletResponse 不能为空");
        Objects.requireNonNull(zipFileName, "下载文件名不能为空");

        if (!Files.exists(source)) {
            throw new IllegalArgumentException("待压缩路径不存在：" + source);
        }

        // 取目录下一层所有文件和目录（非递归）
        List<Path> children = Files.list(source).collect(Collectors.toList());
        if (includeRootDir) {
            zip(Collections.singletonList(source), response, zipFileName);
        } else {
            zip(children, response, zipFileName);
        }

    }

    /**
     * 将多个文件/目录压缩为 zip 文件
     *
     * @param sources   要压缩的文件或目录列表
     * @param zipTarget 目标 zip 文件路径
     * @throws IOException 如果压缩过程中发生 I/O 错误，抛出异常
     */
    public static void zip(List<Path> sources, Path zipTarget) throws IOException {
        Objects.requireNonNull(sources, "要压缩的文件列表不能为空");
        Objects.requireNonNull(zipTarget, "目标 zip 文件不能为空");

        // 创建目标文件的父目录（如果不存在）
        try {
            Files.createDirectories(zipTarget.getParent());
        } catch (IOException e) {
            throw new UncheckedIOException("创建目标文件父目录失败: " + zipTarget, e);
        }

        // 使用 ZipOutputStream 将文件写入 zip 文件
        try (OutputStream fos = Files.newOutputStream(zipTarget);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             ZipOutputStream zos = new ZipOutputStream(bos)) {

            // 设置压缩级别（最高压缩）
            zos.setLevel(Deflater.BEST_COMPRESSION);

            Set<String> entryNames = Collections.newSetFromMap(new ConcurrentHashMap<>());

            // 遍历要压缩的每个文件或目录
            for (Path source : sources) {
                if (!Files.exists(source)) {
                    continue; // 如果文件不存在，跳过
                }
                // 如果源路径本身就是zipTarget，跳过
                if (source.toAbsolutePath().normalize().equals(zipTarget)) {
                    continue;
                }
                Path basePath = source.getParent() != null ? source.getParent() : source;
                zipPath(source, basePath, zos, entryNames); // 压缩路径
            }
        } catch (IOException e) {
            throw new UncheckedIOException("压缩文件时发生错误", e);
        }
    }

    /**
     * 将多个文件/目录压缩并写入到输出流（适用于 Spring Boot 的下载场景）
     *
     * @param sources 文件/目录路径列表
     * @param output  输出流（通常为 HttpServletResponse.getOutputStream）
     * @throws IOException 如果压缩过程中发生 I/O 错误，抛出异常
     */
    public static void zip(List<Path> sources, OutputStream output) throws IOException {
        Objects.requireNonNull(sources, "要压缩的文件列表不能为空");
        Objects.requireNonNull(output, "输出流不能为空");

        // 使用 ZipOutputStream 将文件写入输出流
        try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(output))) {
            // 设置压缩级别（最高压缩）
            zos.setLevel(Deflater.BEST_COMPRESSION);

            Set<String> entryNames = Collections.newSetFromMap(new ConcurrentHashMap<>());

            // 遍历要压缩的每个文件或目录
            for (Path source : sources) {
                if (!Files.exists(source)) {
                    continue; // 如果文件不存在，跳过
                }
                Path basePath = source.getParent() != null ? source.getParent() : source;
                zipPath(source, basePath, zos, entryNames); // 压缩路径
            }
        } catch (IOException e) {
            throw new UncheckedIOException("压缩文件到输出流时发生错误", e);
        }
    }

    /**
     * 将多个文件/目录压缩为 zip 文件并输出到 HttpServletResponse
     *
     * @param sources     要压缩的文件或目录列表
     * @param response    Spring Boot 的 HttpServletResponse
     * @param zipFileName 最终下载的文件名（例如 "files.zip"）
     * @throws IOException 如果压缩或写入过程中发生 I/O 错误，抛出异常
     */
    public static void zip(List<Path> sources, HttpServletResponse response, String zipFileName) throws IOException {
        Objects.requireNonNull(sources, "要压缩的文件列表不能为空");
        Objects.requireNonNull(response, "HttpServletResponse 不能为空");
        Objects.requireNonNull(zipFileName, "下载文件名不能为空");

        // 设置响应头，指示浏览器下载文件
        response.setContentType("application/zip");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + URLEncoder.encode(zipFileName, "UTF-8").replaceAll("\\+", "%20") + "\"");


        // 使用 ZipOutputStream 写入输出流
        try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
            // 设置压缩级别（最高压缩）
            zos.setLevel(Deflater.BEST_COMPRESSION);

            Set<String> entryNames = Collections.newSetFromMap(new ConcurrentHashMap<>());

            // 遍历要压缩的每个文件或目录
            for (Path source : sources) {
                if (!Files.exists(source)) {
                    continue; // 如果文件不存在，跳过
                }
                Path basePath = source.getParent() != null ? source.getParent() : source;
                zipPath(source, basePath, zos, entryNames); // 压缩路径
            }

            // 刷新输出流
            zos.finish();
        } catch (IOException e) {
            throw new UncheckedIOException("压缩文件并写入响应时发生错误", e);
        }
    }

    /**
     * 将多个输入流压缩为 zip 格式并写入到输出流中（适用于 Spring Boot 文件下载）
     *
     * @param fileNames zip 包中的文件名列表（与 streams 一一对应）
     * @param streams   输入流列表（与 fileNames 一一对应）
     * @param output    输出流（通常为 HttpServletResponse.getOutputStream()）
     * @throws IOException 如果压缩或写入时发生错误
     */
    public static void zip(List<String> fileNames, List<InputStream> streams, OutputStream output) throws IOException {
        Objects.requireNonNull(fileNames, "文件名列表不能为空");
        Objects.requireNonNull(streams, "输入流列表不能为空");
        Objects.requireNonNull(output, "输出流不能为空");

        if (fileNames.size() != streams.size()) {
            throw new IllegalArgumentException("文件名列表与输入流列表长度不一致");
        }

        Set<String> entryNames = Collections.newSetFromMap(new ConcurrentHashMap<>());

        try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(output))) {
            zos.setLevel(Deflater.BEST_COMPRESSION);

            for (int i = 0; i < fileNames.size(); i++) {
                String fileName = fileNames.get(i);
                InputStream inputStream = streams.get(i);

                if (fileName == null || inputStream == null) {
                    continue; // 跳过空的项
                }

                // 统一分隔符（兼容 Linux 和 Windows）
                String zipEntryName = fileName.replace(File.separator, "/");

                // 处理重名问题，生成唯一的 zipEntryName
                zipEntryName = resolveDuplicateName(zipEntryName, entryNames);

                zos.putNextEntry(new ZipEntry(zipEntryName));

                try (InputStream is = inputStream) {
                    copyStream(is, zos);
                }

                zos.closeEntry();
            }
        }
    }

    /**
     * 将多个输入流压缩为 zip 文件并写入到指定路径
     */
    public static void zip(List<String> fileNames, List<InputStream> streams, Path zipTarget) throws IOException {
        Objects.requireNonNull(zipTarget, "压缩目标路径不能为空");

        // 确保目标目录存在
        if (zipTarget.getParent() != null) {
            Files.createDirectories(zipTarget.getParent());
        }

        try (OutputStream out = Files.newOutputStream(zipTarget)) {
            zip(fileNames, streams, out);
        }
    }

    /**
     * 内部递归压缩方法
     *
     * @param source   当前路径（文件或目录）
     * @param basePath 用于计算相对路径，防止压缩后路径出错
     * @param zos      ZipOutputStream 实例
     * @throws IOException 如果在压缩过程中发生 I/O 错误，抛出异常
     */
    private static void zipPath(Path source, Path basePath, ZipOutputStream zos) throws IOException {
        // 如果是目录，递归处理子文件
        if (Files.isDirectory(source)) {
            try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(source)) {
                boolean isEmpty = true;
                for (Path subPath : directoryStream) {
                    isEmpty = false;
                    zipPath(subPath, basePath, zos); // 递归压缩子路径
                }

                if (isEmpty) {
                    // 空目录，添加 ZipEntry
                    String dirEntryName = basePath.relativize(source).toString().replace(File.separatorChar, '/') + "/";
                    zos.putNextEntry(new ZipEntry(dirEntryName));
                    zos.closeEntry();
                }
            }
        } else {
            // 计算相对路径
            String zipEntryName = basePath.relativize(source).toString().replace("\\", "/");

            // 创建新的 ZipEntry，并将文件写入 ZipOutputStream
            zos.putNextEntry(new ZipEntry(zipEntryName));
            try (InputStream is = Files.newInputStream(source)) {
                copyStream(is, zos);
            }
            zos.closeEntry(); // 关闭当前 entry
        }
    }

    /**
     * 将指定路径压缩为 ZipEntry，支持处理文件名重复的情况，自动重命名。
     *
     * @param source       待压缩的路径（可以是文件或目录）
     * @param basePath     压缩包中的根路径，用于计算相对路径
     * @param zos          ZipOutputStream 输出流
     * @param addedEntries 已添加的 ZipEntry 名称集合，用于去重
     * @throws IOException IO异常
     */
    private static void zipPath(Path source, Path basePath, ZipOutputStream zos, Set<String> addedEntries) throws IOException {
        if (Files.isDirectory(source)) {
            // 处理目录：递归压缩子文件
            try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(source)) {
                boolean isEmpty = true;

                for (Path subPath : directoryStream) {
                    isEmpty = false;
                    // 递归压缩
                    zipPath(subPath, basePath, zos, addedEntries);
                }

                if (isEmpty) {
                    // 如果是空目录，添加以 / 结尾的 ZipEntry
                    String dirEntryName = basePath.relativize(source).toString().replace(File.separatorChar, '/') + "/";
                    dirEntryName = resolveDuplicateName(dirEntryName, addedEntries);

                    zos.putNextEntry(new ZipEntry(dirEntryName));
                    zos.closeEntry();
                }
            }
        } else {
            // 是文件，生成相对路径（压缩包中的路径）
            String zipEntryName = basePath.relativize(source).toString().replace(File.separatorChar, '/');

            // 处理文件名重复
            zipEntryName = resolveDuplicateName(zipEntryName, addedEntries);

            // 写入 ZipEntry
            zos.putNextEntry(new ZipEntry(zipEntryName));
            try (InputStream is = Files.newInputStream(source)) {
                copyStream(is, zos);
            }
            zos.closeEntry();
        }
    }

    /**
     * 生成一个不重复的 ZipEntry 名称，如果给定的名称已存在，则自动在文件名后加数字后缀，
     * 例如 file.txt、file(1).txt、file(2).txt，依此类推。
     * 目录名处理时保留末尾的斜杠 '/'。
     *
     * @param originalName  原始的 ZipEntry 名称（文件或目录路径，目录以 '/' 结尾）
     * @param existingNames 已经存在的 ZipEntry 名称集合，用于避免重复
     * @return 不重复的唯一 ZipEntry 名称
     */
    private static String resolveDuplicateName(String originalName, Set<String> existingNames) {
        String name = originalName;
        int count = 1;

        String suffix = "";
        // 如果是目录，目录名以 '/' 结尾，先去掉，后面加回去
        if (name.endsWith("/")) {
            name = name.substring(0, name.length() - 1);
            suffix = "/";
        }

        String baseName = name;
        String extension = "";

        // 找最后一个点，拆分文件名和扩展名
        int dotIndex = name.lastIndexOf('.');
        if (dotIndex != -1 && !name.endsWith("/")) {
            baseName = name.substring(0, dotIndex);
            extension = name.substring(dotIndex);
        }

        // 循环直到生成一个不重复的名称
        while (existingNames.contains(name + suffix)) {
            name = baseName + "(" + count + ")" + extension;
            count++;
        }

        // 拼回目录的斜杠
        name = name + suffix;

        // 添加到已存在名称集合
        existingNames.add(name);

        return name;
    }

    /**
     * 解压文件到指定目录
     *
     * @param zipFile   zip 文件路径
     * @param targetDir 解压目标目录
     * @throws IOException 如果解压过程中发生错误，抛出异常
     */
    public static void unzip(Path zipFile, Path targetDir) throws IOException {
        Objects.requireNonNull(zipFile, "zip 文件路径不能为空");
        Objects.requireNonNull(targetDir, "目标目录不能为空");

        // 创建目标目录（如果不存在）
        Files.createDirectories(targetDir);

        try (InputStream fis = Files.newInputStream(zipFile);
             ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis))) {

            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                // 构建目标文件路径
                Path entryPath = targetDir.resolve(entry.getName());

                // 防止 Zip Slip 漏洞
                if (!entryPath.startsWith(targetDir)) {
                    throw new IOException("解压路径非法: " + entry.getName());
                }

                // 如果是目录则创建目录
                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    // 如果是文件，写入文件内容
                    Files.createDirectories(entryPath.getParent());
                    try (OutputStream fos = Files.newOutputStream(entryPath)) {
                        copyStream(zis, fos);
                    }
                }

                zis.closeEntry(); // 关闭当前 entry
            }
        }
    }

    /**
     * 将输入流的数据写入输出流
     *
     * @param inputStream  输入流
     * @param outputStream 输出流
     * @throws IOException 如果发生 I/O 错误，抛出异常
     */
    private static void copyStream(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];  // 使用一个缓冲区提高传输效率
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
    }


}
