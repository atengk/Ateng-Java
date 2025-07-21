package local.ateng.java.awss3.service;

import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Object;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

/**
 * S3 服务接口
 * <p>
 * 提供上传、下载、删除、预签名等常用 S3 操作能力
 *
 * @author
 * @since 2025-07-21
 */
public interface S3Service {

    /**
     * 将 InputStream 转为 byte[]，适合小文件上传
     *
     * @param inputStream 输入流
     * @return 字节数组
     * @throws IOException IO 异常
     */
    byte[] toByteArray(InputStream inputStream) throws IOException;

    /**
     * 上传文件到 S3（通过 InputStream）
     *
     * @param key         文件路径
     * @param inputStream 输入流
     */
    void uploadFile(String key, InputStream inputStream);

    /**
     * 上传文件到 S3（通过 InputStream，带内容长度和类型）
     *
     * @param key           文件路径
     * @param inputStream   输入流
     * @param contentLength 内容长度
     * @param contentType   内容类型
     */
    void uploadFile(String key, InputStream inputStream, long contentLength, String contentType);

    /**
     * 上传文件到 S3（通过字节数组）
     *
     * @param key         文件路径
     * @param data        文件字节内容
     * @param contentType 文件类型
     */
    void uploadFile(String key, byte[] data, String contentType);

    /**
     * 上传本地文件对象到 S3
     *
     * @param key  S3 路径
     * @param file 本地文件对象
     */
    void uploadFile(String key, File file);

    /**
     * 上传 MultipartFile 文件到 S3
     *
     * @param key           S3 路径
     * @param multipartFile Multipart 文件对象
     */
    void uploadFile(String key, MultipartFile multipartFile);

    /**
     * 上传多个 Multipart 文件到 S3
     *
     * @param keys           文件路径集合
     * @param multipartFiles 文件对象集合
     */
    void uploadMultipleFiles(List<String> keys, List<MultipartFile> multipartFiles);

    /**
     * 并发上传多个 Multipart 文件到 S3（默认不忽略错误）
     *
     * @param keys           文件路径集合
     * @param multipartFiles 文件对象集合
     */
    void uploadMultipleFilesAsync(List<String> keys, List<MultipartFile> multipartFiles);

    /**
     * 并发上传多个 Multipart 文件到 S3
     *
     * @param keys           文件路径集合
     * @param multipartFiles 文件对象集合
     * @param ignoreErrors   是否忽略单个上传错误
     */
    void uploadMultipleFilesAsync(List<String> keys, List<MultipartFile> multipartFiles, boolean ignoreErrors);

    /**
     * 上传多个 InputStream 文件流到 S3
     *
     * @param keys         文件路径集合
     * @param inputStreams 输入流集合
     */
    void uploadMultipleFilesWithStreams(List<String> keys, List<InputStream> inputStreams);

    /**
     * 并发上传多个 InputStream 文件到 S3（默认不忽略错误）
     *
     * @param keys         文件路径集合
     * @param inputStreams 输入流集合
     */
    void uploadMultipleFilesAsyncWithStreams(List<String> keys, List<InputStream> inputStreams);

    /**
     * 并发上传多个 InputStream 文件到 S3
     *
     * @param keys         文件路径集合
     * @param inputStreams 输入流集合
     * @param ignoreErrors 是否忽略错误
     */
    void uploadMultipleFilesAsyncWithStreams(List<String> keys, List<InputStream> inputStreams, boolean ignoreErrors);

    /**
     * 下载文件，返回输入流
     *
     * @param key S3 路径
     * @return 输入流
     */
    ResponseInputStream<GetObjectResponse> downloadFile(String key);

    /**
     * 下载文件为 Base64 字符串（无 data: 前缀）
     *
     * @param key S3 路径
     * @return Base64 字符串
     */
    String downloadFileAsBase64(String key);

    /**
     * 下载文件为 Base64 字符串（带 data URI 前缀）
     *
     * @param key S3 路径
     * @return Base64 URI 字符串
     */
    String downloadFileAsBase64Uri(String key);

    /**
     * 将文件写入响应流供下载
     *
     * @param key      S3 路径
     * @param fileName 下载文件名
     * @param response HTTP 响应对象
     */
    void downloadToResponse(String key, String fileName, HttpServletResponse response);

    /**
     * 下载文件并保存到本地路径
     *
     * @param key       S3 路径
     * @param localPath 本地路径
     */
    void downloadToFile(String key, Path localPath);

    /**
     * 批量下载文件并保存到本地路径（默认不忽略错误）
     *
     * @param keys       S3 路径集合
     * @param localPaths 本地路径集合
     */
    void downloadMultipleToFiles(List<String> keys, List<Path> localPaths);

    /**
     * 批量下载文件并保存到本地路径
     *
     * @param keys         S3 路径集合
     * @param localPaths   本地路径集合
     * @param ignoreErrors 是否忽略错误
     */
    void downloadMultipleToFiles(List<String> keys, List<Path> localPaths, boolean ignoreErrors);

    /**
     * 异步批量下载文件（默认不忽略错误）
     *
     * @param keys       S3 路径集合
     * @param localPaths 本地路径集合
     */
    void downloadMultipleToFilesAsync(List<String> keys, List<Path> localPaths);

    /**
     * 异步批量下载文件
     *
     * @param keys         S3 路径集合
     * @param localPaths   本地路径集合
     * @param ignoreErrors 是否忽略错误
     */
    void downloadMultipleToFilesAsync(List<String> keys, List<Path> localPaths, boolean ignoreErrors);

    /**
     * 批量下载文件为输入流集合（默认不忽略错误）
     *
     * @param keys S3 路径集合
     * @return 输入流集合
     */
    List<InputStream> downloadMultipleToStreams(List<String> keys);

    /**
     * 批量下载文件为输入流集合
     *
     * @param keys         S3 路径集合
     * @param ignoreErrors 是否忽略错误
     * @return 输入流集合
     */
    List<InputStream> downloadMultipleToStreams(List<String> keys, boolean ignoreErrors);

    /**
     * 异步批量下载文件为输入流集合（默认不忽略错误）
     *
     * @param keys S3 路径集合
     * @return 输入流集合
     */
    List<InputStream> downloadMultipleToStreamsAsync(List<String> keys);

    /**
     * 异步批量下载文件为输入流集合
     *
     * @param keys         S3 路径集合
     * @param ignoreErrors 是否忽略错误
     * @return 输入流集合
     */
    List<InputStream> downloadMultipleToStreamsAsync(List<String> keys, boolean ignoreErrors);

    /**
     * 删除单个文件
     *
     * @param key 文件路径
     */
    void deleteFile(String key);

    /**
     * 批量删除文件
     *
     * @param keys 文件路径集合
     */
    void deleteFiles(List<String> keys);

    /**
     * 递归删除指定前缀的所有对象（模拟删除文件夹）
     *
     * @param prefix 路径前缀
     */
    void deleteFolderRecursively(String prefix);

    /**
     * 判断对象是否存在
     *
     * @param key 文件路径
     * @return 是否存在
     */
    boolean doesObjectExist(String key);

    /**
     * 列出指定前缀下的所有文件
     *
     * @param prefix 路径前缀
     * @return S3 文件对象列表
     */
    List<S3Object> listFiles(String prefix);

    /**
     * 生成临时访问链接（GET）
     *
     * @param key      文件路径
     * @param duration 有效时长
     * @return 临时访问 URL
     */
    String generatePresignedUrl(String key, Duration duration);

    /**
     * 生成临时上传链接（PUT）
     *
     * @param key      文件路径
     * @param duration 有效时长
     * @return 临时上传 URL
     */
    String generatePresignedUploadUrl(String key, Duration duration);

    /**
     * 生成公开桶中文件的访问链接（直链）
     *
     * @param key 文件路径
     * @return 公开访问 URL
     */
    String generatePublicUrl(String key);
}
