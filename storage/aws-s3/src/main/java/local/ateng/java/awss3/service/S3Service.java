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
import java.util.Map;

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
     * 根据原始文件名生成唯一的 S3 Key，结构如：
     * upload/202508/uuid.jpg
     *
     * @param originalFilename 原始文件名（如 "test.jpg"）
     * @return S3 key（如 "upload/202508/1cc2fa37-bdf3-4fbe-a7a4-83c07b803c8e.jpg"）
     */
    String generateKey(String originalFilename);

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
     * 上传 MultipartFile 文件到 S3，生成默认路径
     *
     * @param multipartFile Multipart 文件对象
     * @return 上传后的地址
     */
    String uploadFile(MultipartFile multipartFile);

    /**
     * 上传 MultipartFile 文件到 S3
     *
     * @param key           S3 路径
     * @param multipartFile Multipart 文件对象
     */
    void uploadFile(String key, MultipartFile multipartFile);

    /**
     * 上传 MultipartFile 文件到 S3
     *
     * @param key           S3 路径
     * @param multipartFile Multipart 文件对象
     * @param metadata      文件 Metadata 元数据，key 必须以小写英文字母、数字、连字符组成，value 必须是 ASCII 编码
     */
    void uploadFile(String key, MultipartFile multipartFile, Map<String, String> metadata);

    /**
     * 从 S3 中获取指定对象的元数据信息，并自动尝试对值进行 Base64 解码还原原始内容。
     * <p>
     * 如果元数据值是上传时经 Base64 编码的内容（如包含中文），则会自动解码为原始字符串；
     * 否则保留原值。
     *
     * @param key S3 对象的键（文件路径）
     * @return 解码后的元数据映射
     */
    Map<String, String> getDecodedMetadata(String key);

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
     * 下载 S3 中指定前缀下的所有文件到本地，并保持目录结构
     *
     * @param prefix       S3 目录前缀（如 "folder/sub/"）
     * @param localBaseDir 本地基础目录（如 "D:/downloads"）
     */
    void downloadFolder(String prefix, Path localBaseDir);

    /**
     * 上传本地目录到 S3 中指定前缀路径下，保留原有目录结构
     *
     * @param localBaseDir 本地基础目录（如 "D:/upload"）
     * @param prefix     S3 中的存储前缀（如 "backup/2025/"）
     */
    void uploadFolder(Path localBaseDir, String prefix);

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
     * @return S3 文件列表
     */
    List<S3Object> listFiles(String prefix);

    /**
     * 列出指定前缀下的所有文件
     *
     * @param prefix 路径前缀
     * @return S3 文件列表
     */
    List<String> listFilesStr(String prefix);

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
