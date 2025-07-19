package local.ateng.java.awss3.utils;


import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Zip 工具类：支持将多个文件或目录压缩为 zip 文件或输出流
 * 示例：
 * 压缩目录：ZipUtil.zip(Collections.singletonList(Paths.get("D:\\temp\\zip")), Paths.get("D:\\temp\\zip.zip"));
 * 压缩文件：ZipUtil.zip(Arrays.asList(Paths.get("D:\\temp\\20250718\\1.jpg"), Paths.get("D:\\temp\\20250718\\2.jpg")), Paths.get("D:\\temp\\zip.zip"));
 * 压缩文件和目录（混合）：ZipUtil.zip(Arrays.asList(Paths.get("D:\\temp\\20250718\\1.jpg"), Paths.get("D:\\temp\\20250718\\2.jpg"), Paths.get("D:\\temp\\20240422")), Paths.get("D:\\temp\\zip.zip"));
 * 解压：ZipUtil.unzip(Paths.get("D:\\temp\\zip.zip"), Paths.get("D:\\temp\\zip"));
 *
 * @author Ateng
 * @since 2025-07-19
 */
public class ZipUtil {

    /**
     * 缓冲区大小
     */
    private static final int BUFFER_SIZE = 8192;

    private ZipUtil() {
        // 工具类不能实例化
        throw new UnsupportedOperationException("工具类不可实例化");
    }

    public static void main(String[] args) throws IOException {
        ZipUtil.unzip(Paths.get("D:\\temp\\zip.zip"), Paths.get("D:\\temp\\zip"));
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

            // 遍历要压缩的每个文件或目录
            for (Path source : sources) {
                if (!Files.exists(source)) {
                    continue; // 如果文件不存在，跳过
                }
                Path basePath = source.getParent() != null ? source.getParent() : source;
                zipPath(source, basePath, zos); // 压缩路径
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

            // 遍历要压缩的每个文件或目录
            for (Path source : sources) {
                if (!Files.exists(source)) {
                    continue; // 如果文件不存在，跳过
                }
                Path basePath = source.getParent() != null ? source.getParent() : source;
                zipPath(source, basePath, zos); // 压缩路径
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
    public static void writeZipToResponse(List<Path> sources, HttpServletResponse response, String zipFileName) throws IOException {
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

            // 遍历要压缩的每个文件或目录
            for (Path source : sources) {
                if (!Files.exists(source)) {
                    continue; // 如果文件不存在，跳过
                }
                Path basePath = source.getParent() != null ? source.getParent() : source;
                zipPath(source, basePath, zos); // 压缩路径
            }

            // 刷新输出流
            zos.finish();
        } catch (IOException e) {
            throw new UncheckedIOException("压缩文件并写入响应时发生错误", e);
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
                for (Path subPath : directoryStream) {
                    zipPath(subPath, basePath, zos); // 递归压缩
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
