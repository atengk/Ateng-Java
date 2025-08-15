package local.ateng.java.awss3.service.impl;

import local.ateng.java.awss3.config.S3Properties;
import local.ateng.java.awss3.service.S3Service;
import local.ateng.java.awss3.utils.FileUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * S3 服务类
 *
 * @author Ateng
 * @since 2025-07-18
 */
@Service
@RequiredArgsConstructor
public class S3ServiceImpl implements S3Service {

    private static final Logger log = LoggerFactory.getLogger(S3ServiceImpl.class);

    /**
     * 缓冲区大小
     */
    private static final int BUFFER_SIZE = 8192;

    private final S3Client s3Client;
    private final S3Properties s3Properties;
    private final S3Presigner s3Presigner;

    /**
     * 将 InputStream 转为 byte[]，适合小文件上传
     *
     * @param inputStream 输入流
     * @return 字节数组
     * @throws IOException IO 异常
     */
    @Override
    public byte[] toByteArray(InputStream inputStream) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            return baos.toByteArray();
        }
    }

    /**
     * 根据原始文件名生成唯一的 S3 Key，结构如：
     * upload/202508/uuid.jpg
     *
     * @param originalFilename 原始文件名（如 "test.jpg"）
     * @return S3 key（如 "upload/202508/1cc2fa37-bdf3-4fbe-a7a4-83c07b803c8e.jpg"）
     */
    @Override
    public String generateKey(String originalFilename) {
        // 日期路径：202508
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));

        // UUID 文件名（保留原后缀）
        String suffix = FileUtil.getFileExtension(originalFilename, true);
        String mimeCategory = FileUtil.getMimeCategory(suffix);

        String uuid = UUID.randomUUID().toString();

        return String.format("upload/%s/%s/%s%s", mimeCategory, datePath, uuid, suffix);
    }

    /**
     * 上传文件到 S3
     *
     * <p>该方法是最通用的文件上传方式，只要求提供 S3 的 Key 和输入流。
     * 会自动尝试读取输入流为字节数组上传，适合小中型文件。</p>
     *
     * @param key         文件在 S3 中的完整路径（如：folder/test.pdf）
     * @param inputStream 输入流，来自文件、网络或内存
     * @throws RuntimeException 读取失败或上传失败时抛出
     */
    @Override
    public void uploadFile(String key, InputStream inputStream) {
        try {
            // 读取输入流为字节数组
            byte[] data = toByteArray(inputStream);

            PutObjectRequest request = PutObjectRequest
                    .builder()
                    .bucket(s3Properties.getBucketName())
                    .key(key)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM_VALUE)
                    .contentLength((long) data.length)
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(data));
        } catch (Exception e) {
            log.error("S3文件上传失败，bucket={}, key={}", s3Properties.getBucketName(), key, e);
            throw new RuntimeException("S3文件上传失败", e);
        }
    }


    /**
     * 上传文件到 S3（通过 InputStream）
     *
     * @param key           文件在 S3 中的完整路径（例如：folder/test.txt）
     * @param inputStream   输入流，文件内容
     * @param contentLength 文件长度（单位：字节）
     * @param contentType   文件类型（如 "application/pdf", "image/jpeg"）
     */
    @Override
    public void uploadFile(String key, InputStream inputStream, long contentLength, String contentType) {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(s3Properties.getBucketName())
                    .key(key)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(request, RequestBody.fromInputStream(inputStream, contentLength));
        } catch (Exception e) {
            log.error("S3文件上传失败，bucket={}, key={}", s3Properties.getBucketName(), key, e);
            throw new RuntimeException("S3文件上传失败", e);
        }
    }

    /**
     * 上传文件到 S3（通过字节数组）
     *
     * @param key         文件路径
     * @param data        文件字节内容
     * @param contentType 文件类型（如 "application/json"）
     */
    @Override
    public void uploadFile(String key, byte[] data, String contentType) {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(s3Properties.getBucketName())
                    .key(key)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(data));
        } catch (Exception e) {
            log.error("S3文件上传失败，bucket={}, key={}", s3Properties.getBucketName(), key, e);
            throw new RuntimeException("S3文件上传失败", e);
        }
    }

    /**
     * 上传文件到 S3（通过本地文件 File 对象）
     *
     * @param key  目标路径（包含文件名）
     * @param file 本地文件对象
     */
    @Override
    public void uploadFile(String key, File file) {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(s3Properties.getBucketName())
                    .key(key)
                    .build();

            s3Client.putObject(request, RequestBody.fromFile(file));
        } catch (Exception e) {
            log.error("S3文件上传失败，bucket={}, key={}", s3Properties.getBucketName(), key, e);
            throw new RuntimeException("S3文件上传失败", e);
        }
    }

    /**
     * 上传 MultipartFile 文件到 S3，生成默认路径
     *
     * @param multipartFile Multipart 文件对象
     * @return 上传后的地址
     */
    @Override
    public String uploadFile(MultipartFile multipartFile) {
        String path = generateKey(multipartFile.getOriginalFilename());
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(s3Properties.getBucketName())
                    .key(path)
                    .contentType(multipartFile.getContentType())
                    .build();

            s3Client.putObject(request, RequestBody.fromInputStream(multipartFile.getInputStream(), multipartFile.getSize()));
        } catch (Exception e) {
            log.error("S3文件上传失败，bucket={}, key={}", s3Properties.getBucketName(), path, e);
            throw new RuntimeException("S3文件上传失败", e);
        }
        return path;
    }

    /**
     * 上传文件到 S3（处理来自前端 Multipart 请求）
     *
     * @param key           上传目标路径（S3 中的 key）
     * @param multipartFile Spring MVC 接收到的文件对象
     * @throws RuntimeException 上传失败抛出异常
     */
    @Override
    public void uploadFile(String key, MultipartFile multipartFile) {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(s3Properties.getBucketName())
                    .key(key)
                    .contentType(multipartFile.getContentType())
                    .build();

            s3Client.putObject(request, RequestBody.fromInputStream(multipartFile.getInputStream(), multipartFile.getSize()));
        } catch (Exception e) {
            log.error("S3文件上传失败，bucket={}, key={}", s3Properties.getBucketName(), key, e);
            throw new RuntimeException("S3文件上传失败", e);
        }
    }

    /**
     * 上传 MultipartFile 文件到 S3
     *
     * @param key           S3 路径
     * @param multipartFile Multipart 文件对象
     * @param metadata      文件 Metadata 元数据，key 必须以小写英文字母、数字、连字符组成，value 必须是 ASCII 编码
     */
    @Override
    public void uploadFile(String key, MultipartFile multipartFile, Map<String, String> metadata) {
        try {
            Map<String, String> sanitizeMetadata = sanitizeMetadata(metadata);
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(s3Properties.getBucketName())
                    .key(key)
                    .contentType(multipartFile.getContentType())
                    .metadata(sanitizeMetadata)
                    .build();

            s3Client.putObject(request, RequestBody.fromInputStream(multipartFile.getInputStream(), multipartFile.getSize()));
        } catch (Exception e) {
            log.error("S3文件上传失败，bucket={}, key={}", s3Properties.getBucketName(), key, e);
            throw new RuntimeException("S3文件上传失败", e);
        }
    }

    /**
     * 对自定义的 S3 元数据进行清洗和编码处理。
     * <p>
     * 所有键统一转换为小写，值中若包含非 ASCII 字符（如中文）将使用 Base64 编码，
     * 以避免签名计算错误导致上传失败。
     *
     * @param metadata 原始元数据映射
     * @return 处理后的安全元数据映射，适用于 S3 上传
     */
    private Map<String, String> sanitizeMetadata(Map<String, String> metadata) {
        Map<String, String> sanitized = new HashMap<>();
        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            // 保证 key 为小写
            String key = entry.getKey().toLowerCase();
            String value = entry.getValue();

            // 判断 value 是否为 ASCII（英文字符）
            if (StandardCharsets.US_ASCII.newEncoder().canEncode(value)) {
                sanitized.put(key, value);
            } else {
                // 非 ASCII 的值进行 Base64 编码
                String encoded = Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
                sanitized.put(key, encoded);
            }
        }
        return sanitized;
    }

    /**
     * 从 S3 中获取指定对象的元数据信息，并自动尝试对值进行 Base64 解码还原原始内容。
     * <p>
     * 如果元数据值是上传时经 Base64 编码的内容（如包含中文），则会自动解码为原始字符串；
     * 否则保留原值。
     *
     * @param key S3 对象的键（文件路径）
     * @return 解码后的元数据映射
     */
    @Override
    public Map<String, String> getDecodedMetadata(String key) {
        HeadObjectRequest headRequest = HeadObjectRequest.builder()
                .bucket(s3Properties.getBucketName())
                .key(key)
                .build();

        HeadObjectResponse response = s3Client.headObject(headRequest);

        Map<String, String> originalMetadata = new HashMap<>();

        for (Map.Entry<String, String> entry : response.metadata().entrySet()) {
            String keyName = entry.getKey();
            String value = entry.getValue();

            // 尝试 Base64 解码（有些值是英文直接传输的）
            String decoded;
            try {
                byte[] decodedBytes = Base64.getDecoder().decode(value);
                decoded = new String(decodedBytes, StandardCharsets.UTF_8);

                // 只有在成功解码为有效 UTF-8 后才认为是原始值
                if (isUtf8(decoded)) {
                    originalMetadata.put(keyName, decoded);
                } else {
                    // 保留原始值
                    originalMetadata.put(keyName, value);
                }
            } catch (IllegalArgumentException e) {
                // 不是合法的 Base64，说明本来就是 ASCII
                originalMetadata.put(keyName, value);
            }
        }

        return originalMetadata;
    }

    /**
     * 判断给定的字符串是否可以用 UTF-8 编码。
     * <p>
     * 可用于验证 Base64 解码后的字符串是否是有效的 UTF-8 格式。
     *
     * @param text 待验证的字符串
     * @return 如果是合法的 UTF-8 字符串则返回 true，否则返回 false
     */
    private boolean isUtf8(String text) {
        CharsetEncoder encoder = StandardCharsets.UTF_8.newEncoder();
        return encoder.canEncode(text);
    }

    /**
     * 上传多个文件到 S3（处理来自前端 Multipart 请求）
     *
     * @param keys           上传目标路径集合（S3 中的多个 key）
     * @param multipartFiles Spring MVC 接收到的文件对象集合
     * @throws RuntimeException 上传失败抛出异常
     */
    @Override
    public void uploadMultipleFiles(List<String> keys, List<MultipartFile> multipartFiles) {
        if (keys.size() != multipartFiles.size()) {
            throw new IllegalArgumentException("上传的文件路径和文件数量不匹配！");
        }
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            MultipartFile multipartFile = multipartFiles.get(i);
            uploadFile(key, multipartFile);
        }
    }

    /**
     * 并发上传多个文件到 S3（处理来自前端 Multipart 请求）
     * 使用默认线程池（ForkJoinPool.commonPool）
     *
     * @param keys           上传目标路径集合（S3 中的多个 key）
     * @param multipartFiles Spring MVC 接收到的文件对象集合
     * @throws RuntimeException 上传失败抛出异常
     */
    @Override
    public void uploadMultipleFilesAsync(List<String> keys, List<MultipartFile> multipartFiles) {
        uploadMultipleFilesAsync(keys, multipartFiles, false);
    }

    /**
     * 并发上传多个文件到 S3（处理来自前端 Multipart 请求）
     * 支持忽略单个上传错误（通过 ignoreErrors 参数控制）
     * 使用默认线程池（ForkJoinPool.commonPool）
     *
     * @param keys           上传目标路径集合（S3 中的多个 key）
     * @param multipartFiles Spring MVC 接收到的文件对象集合
     * @param ignoreErrors   是否忽略单个文件上传错误，true 表示继续上传其他文件
     */
    @Override
    public void uploadMultipleFilesAsync(List<String> keys, List<MultipartFile> multipartFiles, boolean ignoreErrors) {
        if (keys.size() != multipartFiles.size()) {
            throw new IllegalArgumentException("上传的文件路径和文件数量不匹配！");
        }

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 0; i < keys.size(); i++) {
            final String key = keys.get(i);
            final MultipartFile file = multipartFiles.get(i);

            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    uploadFile(key, file);
                } catch (RuntimeException e) {
                    if (!ignoreErrors) {
                        throw e;
                    } else {
                        System.err.println("上传失败: " + key + ", 错误: " + e.getMessage());
                    }
                }
            });

            futures.add(future);
        }

        // 等待所有上传任务完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    /**
     * 上传多个文件到 S3（处理来自前端的 InputStream 文件）
     *
     * @param keys         上传目标路径集合（S3 中的多个 key）
     * @param inputStreams 输入流集合（每个流代表一个文件）
     * @throws RuntimeException 上传失败时抛出异常
     */
    @Override
    public void uploadMultipleFilesWithStreams(List<String> keys, List<InputStream> inputStreams) {
        if (keys.size() != inputStreams.size()) {
            throw new IllegalArgumentException("上传的文件路径和文件数量不匹配！");
        }

        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            InputStream inputStream = inputStreams.get(i);
            uploadFile(key, inputStream);
        }
    }

    /**
     * 并发上传多个文件到 S3（处理来自前端的 InputStream 文件）
     * 使用默认线程池（ForkJoinPool.commonPool）
     *
     * @param keys         上传目标路径集合（S3 中的多个 key）
     * @param inputStreams 输入流集合（每个流代表一个文件）
     * @throws RuntimeException 上传失败时抛出异常
     */
    @Override
    public void uploadMultipleFilesAsyncWithStreams(List<String> keys, List<InputStream> inputStreams) {
        uploadMultipleFilesAsyncWithStreams(keys, inputStreams, false);
    }

    /**
     * 并发上传多个文件到 S3（处理来自前端的 InputStream 文件）
     * 支持忽略单个上传错误（通过 ignoreErrors 参数控制）
     * 使用默认线程池（ForkJoinPool.commonPool）
     *
     * @param keys         上传目标路径集合（S3 中的多个 key）
     * @param inputStreams 输入流集合（每个流代表一个文件）
     * @param ignoreErrors 是否忽略单个文件上传错误，true 表示继续上传其他文件
     */
    @Override
    public void uploadMultipleFilesAsyncWithStreams(List<String> keys, List<InputStream> inputStreams, boolean ignoreErrors) {
        if (keys.size() != inputStreams.size()) {
            throw new IllegalArgumentException("上传的文件路径和文件数量不匹配！");
        }

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 0; i < keys.size(); i++) {
            final String key = keys.get(i);
            final InputStream inputStream = inputStreams.get(i);

            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    uploadFile(key, inputStream);
                } catch (RuntimeException e) {
                    if (!ignoreErrors) {
                        throw e;  // 如果不忽略错误，抛出异常
                    } else {
                        System.err.println("上传失败: " + key + ", 错误: " + e.getMessage());
                    }
                }
            });

            futures.add(future);
        }

        // 等待所有上传任务完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    /**
     * 下载文件，返回 S3 响应输入流
     *
     * @param key S3 文件路径
     * @return 包含响应头的输入流，可用于保存或转发
     */
    @Override
    public ResponseInputStream<GetObjectResponse> downloadFile(String key) {
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(s3Properties.getBucketName())
                    .key(key)
                    .build();

            return s3Client.getObject(request);
        } catch (Exception e) {
            log.error("S3文件下载失败，bucket={}, key={}", s3Properties.getBucketName(), key, e);
            throw new RuntimeException("S3文件下载失败", e);
        }
    }

    /**
     * 下载文件并返回 Base64 编码字符串（不包含 data 前缀）
     *
     * @param key S3 文件路径
     * @return Base64 编码后的字符串（如：iVBORw0KGgoAAAANS...）
     */
    @Override
    public String downloadFileAsBase64(String key) {
        try (ResponseInputStream<GetObjectResponse> s3Stream = downloadFile(key)) {
            byte[] bytes = toByteArray(s3Stream);
            return Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            log.error("S3文件下载或转换为Base64失败，bucket={}, key={}", s3Properties.getBucketName(), key, e);
            throw new RuntimeException("S3文件下载或转换为Base64失败", e);
        }
    }

    /**
     * 下载文件并返回带 data: 前缀的 Base64 URI 字符串
     *
     * @param key S3 文件路径
     * @return Base64 URI 字符串（如：data:image/png;base64,iVBORw0KGgoAAAANS...）
     */
    @Override
    public String downloadFileAsBase64Uri(String key) {
        try (ResponseInputStream<GetObjectResponse> s3Stream = downloadFile(key)) {
            byte[] bytes = toByteArray(s3Stream);
            String contentType = s3Stream.response().contentType();
            return "data:" + contentType + ";base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            log.error("S3文件下载或转换为Base64 URI失败，bucket={}, key={}", s3Properties.getBucketName(), key, e);
            throw new RuntimeException("S3文件下载或转换为Base64 URI失败", e);
        }
    }

    /**
     * 下载文件并写入响应流，用于浏览器下载
     *
     * @param key      S3 文件路径
     * @param fileName 下载时的文件名
     * @param response HttpServletResponse
     */
    @Override
    public void downloadToResponse(String key, String fileName, HttpServletResponse response) {
        try (ResponseInputStream<GetObjectResponse> s3Stream = downloadFile(key);
             OutputStream out = response.getOutputStream()) {

            GetObjectResponse objectResponse = s3Stream.response();

            response.setContentType(objectResponse.contentType() != null ? objectResponse.contentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE);
            response.setHeader("Content-Disposition", "attachment; filename=\"" + URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString()).replaceAll("\\+", "%20") + "\"");
            response.setHeader("Content-Length", String.valueOf(objectResponse.contentLength()));

            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = s3Stream.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }

            out.flush();
        } catch (Exception e) {
            log.error("S3文件下载到响应失败，bucket={}, key={}", s3Properties.getBucketName(), key, e);
            throw new RuntimeException("S3文件下载到响应失败", e);
        }
    }


    /**
     * 下载文件并保存到本地路径
     *
     * @param key       S3 路径
     * @param localPath 本地保存路径
     */
    @Override
    public void downloadToFile(String key, Path localPath) {
        try (ResponseInputStream<GetObjectResponse> s3Stream = downloadFile(key)) {
            Path parentDir = localPath.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }

            Files.copy(s3Stream, localPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            log.error("S3文件下载到本地失败，bucket={}, key={}", s3Properties.getBucketName(), key, e);
            throw new RuntimeException("S3文件下载到本地失败", e);
        }
    }

    /**
     * 批量下载多个文件并保存到本地路径
     *
     * @param keys         S3 文件路径列表
     * @param localPaths   本地保存路径列表
     * @param ignoreErrors 是否忽略下载失败；true 表示忽略，false 表示遇到失败立即抛异常
     */
    @Override
    public void downloadMultipleToFiles(List<String> keys, List<Path> localPaths, boolean ignoreErrors) {
        if (keys.size() != localPaths.size()) {
            throw new IllegalArgumentException("S3对象数量与本地路径数量不匹配");
        }

        List<Path> downloaded = new CopyOnWriteArrayList<>();

        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            Path localPath = localPaths.get(i);

            try {
                downloadToFile(key, localPath);
                downloaded.add(localPath);
            } catch (Exception e) {
                if (!ignoreErrors) {
                    // 下载失败时清理已下载的文件
                    for (Path path : downloaded) {
                        try {
                            Files.deleteIfExists(path);
                        } catch (Exception ex) {
                            log.warn("清理已下载文件失败，path={}", path, ex);
                        }
                    }
                    log.error("S3批量下载中出现错误，bucket={}, key={}", s3Properties.getBucketName(), key, e);
                    throw new RuntimeException("S3批量下载失败，已回滚已下载文件", e);
                } else {
                    log.warn("下载失败（已忽略），bucket={}, key={}, 原因={}", s3Properties.getBucketName(), key, e.getMessage());
                }
            }
        }
    }

    /**
     * 批量下载多个文件并保存到本地路径
     *
     * @param keys       S3 文件路径列表
     * @param localPaths 本地保存路径列表
     */
    @Override
    public void downloadMultipleToFiles(List<String> keys, List<Path> localPaths) {
        downloadMultipleToFiles(keys, localPaths, false);
    }

    /**
     * 异步并发下载多个文件并保存到本地路径
     * 使用默认线程池（ForkJoinPool.commonPool）
     *
     * @param keys       S3 文件路径列表
     * @param localPaths 本地保存路径列表
     */
    @Override
    public void downloadMultipleToFilesAsync(List<String> keys, List<Path> localPaths) {
        downloadMultipleToFilesAsync(keys, localPaths, false);
    }

    /**
     * 并发下载多个文件并保存到本地路径
     * 支持如果某个文件下载失败时可以选择忽略错误
     * 如果下载失败，则会清理已下载的文件
     * 使用默认线程池（ForkJoinPool.commonPool）
     *
     * @param keys         S3 文件路径列表
     * @param localPaths   本地保存路径列表
     * @param ignoreErrors 是否忽略单个文件下载错误，默认为不忽略
     */
    @Override
    public void downloadMultipleToFilesAsync(List<String> keys, List<Path> localPaths, boolean ignoreErrors) {
        if (keys.size() != localPaths.size()) {
            throw new IllegalArgumentException("S3对象数量与本地路径数量不匹配");
        }

        List<CompletableFuture<Void>> tasks = new ArrayList<>();
        List<Path> downloaded = new CopyOnWriteArrayList<>();

        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            Path path = localPaths.get(i);

            CompletableFuture<Void> task = CompletableFuture.runAsync(() -> {
                try {
                    downloadToFile(key, path);
                    // 成功下载，记录文件路径
                    downloaded.add(path);
                } catch (RuntimeException e) {
                    if (!ignoreErrors) {
                        // 如果不忽略错误，则抛出异常，停止其他文件下载
                        throw e;
                    } else {
                        // 如果忽略错误，则打印错误并继续其他任务
                        log.warn("异步下载失败（已忽略），bucket={}, key={}, path={}, 原因={}",
                                s3Properties.getBucketName(), key, path, e.getMessage());
                    }
                }
            });

            tasks.add(task);
        }

        // 等待所有任务完成（或抛出异常）
        try {
            CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0])).join();
        } catch (Exception e) {
            // 如果遇到错误，清理已成功下载的文件
            for (Path path : downloaded) {
                try {
                    Files.deleteIfExists(path);
                } catch (Exception ex) {
                    log.warn("清理已下载文件失败，path={}", path, ex);
                }
            }
            log.error("S3批量异步下载失败，bucket={}", s3Properties.getBucketName(), e);
            throw new RuntimeException("S3批量异步下载失败，已回滚已下载文件", e);
        }
    }

    /**
     * 批量下载多个 S3 文件为输入流列表
     *
     * @param keys         S3 文件路径列表
     * @param ignoreErrors 是否忽略下载失败的文件；true 表示忽略，false 表示遇到失败立即抛异常
     * @return 成功下载的输入流列表（顺序与成功的 key 保持一致）
     */
    @Override
    public List<InputStream> downloadMultipleToStreams(List<String> keys, boolean ignoreErrors) {
        List<InputStream> inputStreams = new ArrayList<>();

        for (String key : keys) {
            try {
                InputStream is = downloadFile(key);
                inputStreams.add(is);
            } catch (Exception e) {
                if (!ignoreErrors) {
                    // 关闭之前已打开的流，避免资源泄露
                    for (InputStream opened : inputStreams) {
                        try {
                            opened.close();
                        } catch (Exception ex) {
                            log.warn("关闭已打开流失败", ex);
                        }
                    }
                    log.error("S3批量下载流失败，bucket={}, key={}", s3Properties.getBucketName(), key, e);
                    throw new RuntimeException("S3批量下载流失败", e);
                } else {
                    log.warn("下载失败（已忽略），bucket={}, key={}, 原因={}", s3Properties.getBucketName(), key, e.getMessage());
                }
            }
        }

        return inputStreams;
    }

    /**
     * 批量下载多个 S3 文件并返回对应的输入流列表
     *
     * @param keys S3 文件路径列表
     * @return 对应文件内容的输入流列表（与 keys 一一对应）
     * @throws RuntimeException 任一文件下载失败将抛出异常
     */
    @Override
    public List<InputStream> downloadMultipleToStreams(List<String> keys) {
        return downloadMultipleToStreams(keys, false);
    }

    /**
     * 并发下载多个 S3 文件为输入流列表
     * 支持忽略下载失败的文件；true 表示忽略，false 表示遇到失败立即抛异常
     *
     * @param keys         S3 文件路径列表
     * @param ignoreErrors 是否忽略下载失败的文件；true 表示忽略，false 表示遇到失败立即抛异常
     * @return 成功下载的输入流列表（顺序与成功的 key 保持一致）
     */
    @Override
    public List<InputStream> downloadMultipleToStreamsAsync(List<String> keys, boolean ignoreErrors) {
        List<CompletableFuture<InputStream>> futures = new ArrayList<>();

        // 提交异步任务
        for (String key : keys) {
            CompletableFuture<InputStream> future = CompletableFuture.supplyAsync(() -> {
                try {
                    return downloadFile(key);  // 假设 downloadFile 方法返回文件的 InputStream
                } catch (Exception e) {
                    if (!ignoreErrors) {
                        log.error("异步下载失败，bucket={}, key={}", s3Properties.getBucketName(), key, e);
                        throw new RuntimeException("S3异步下载失败", e);
                    } else {
                        log.warn("异步下载失败（已忽略），bucket={}, key={}, 原因={}", s3Properties.getBucketName(), key, e.getMessage());
                        return null;
                    }
                }
            });
            futures.add(future);
        }

        // 等待所有任务完成并收集结果
        List<InputStream> inputStreams = new CopyOnWriteArrayList<>();

        for (CompletableFuture<InputStream> future : futures) {
            try {
                // 获取每个 Future 的结果（InputStream）
                InputStream inputStream = future.join();
                if (inputStream != null) {
                    inputStreams.add(inputStream);
                }
            } catch (CompletionException e) {
                if (!ignoreErrors) {
                    // 如果抛出异常，并且没有忽略错误，重新抛出异常中断任务
                    log.error("异步任务执行失败，bucket={}", s3Properties.getBucketName(), e.getCause());
                    throw new RuntimeException("S3异步任务执行失败", e.getCause());
                } else {
                    // 如果忽略错误，则打印错误
                    log.warn("异步任务执行失败（已忽略），bucket={}, 原因={}", s3Properties.getBucketName(), e.getCause().getMessage());
                }
            }
        }

        return inputStreams;
    }

    @Override
    public void downloadFolder(String prefix, Path localBaseDir) {
        List<S3Object> objects = listFiles(prefix);
        if (objects.isEmpty()) {
            log.info("S3路径下无文件，bucket={}, prefix={}", s3Properties.getBucketName(), prefix);
            return;
        }

        for (S3Object object : objects) {
            String key = object.key();

            // 去掉 prefix 得到相对路径（保留目录结构）
            String relativePath = key.substring(prefix.length());
            Path localPath = localBaseDir.resolve(relativePath);

            // 判断是否已存在，并且大小一致，若一致则跳过
            if (Files.exists(localPath)) {
                try {
                    long localSize = Files.size(localPath);
                    long s3Size = object.size();

                    if (localSize == s3Size) {
                        log.info("文件已存在且大小一致，跳过下载，path={}", localPath);
                        continue;
                    } else {
                        log.info("文件已存在但大小不一致，重新下载，path={}", localPath);
                    }
                } catch (Exception e) {
                    log.warn("读取本地文件大小失败，强制重新下载，path={}", localPath, e);
                }
            }

            // 创建父目录
            try {
                Files.createDirectories(localPath.getParent());
            } catch (Exception e) {
                log.error("创建本地目录失败，path={}", localPath.getParent(), e);
                throw new RuntimeException("创建本地目录失败", e);
            }

            // 下载文件
            downloadToFile(key, localPath);
        }
    }

    @Override
    public void uploadFolder(Path localBaseDir, String prefix) {
        if (!Files.isDirectory(localBaseDir)) {
            throw new IllegalArgumentException("指定路径不是目录：" + localBaseDir);
        }

        try (Stream<Path> stream = Files.walk(localBaseDir)) {
            stream.filter(Files::isRegularFile)
                    .filter(Files::isRegularFile)
                    .forEach(path -> {
                        // 获取相对路径并转为 S3 key
                        Path relative = localBaseDir.relativize(path);
                        String s3Key = prefix + (prefix.endsWith("/") ? "" : "/") + relative.toString().replace("\\", "/");

                        // 上传文件
                        uploadFile(s3Key, path.toFile());
                    });
        } catch (Exception e) {
            log.error("遍历本地目录失败，path={}", localBaseDir, e);
            throw new RuntimeException("遍历本地目录失败", e);
        }
    }

    /**
     * 批量下载多个 S3 文件并返回对应的输入流列表（默认不忽略错误）
     *
     * @param keys S3 文件路径列表
     * @return 对应文件内容的输入流列表（与 keys 一一对应）
     * @throws RuntimeException 任一文件下载失败将抛出异常
     */
    @Override
    public List<InputStream> downloadMultipleToStreamsAsync(List<String> keys) {
        return downloadMultipleToStreamsAsync(keys, false);
    }

    /**
     * 删除单个文件
     *
     * @param key 文件路径
     */
    @Override
    public void deleteFile(String key) {
        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(s3Properties.getBucketName())
                    .key(key)
                    .build();

            s3Client.deleteObject(request);
            log.info("S3文件删除成功，bucket={}, key={}", s3Properties.getBucketName(), key);
        } catch (Exception e) {
            log.error("S3文件删除失败，bucket={}, key={}", s3Properties.getBucketName(), key, e);
            throw new RuntimeException("S3文件删除失败", e);
        }
    }

    /**
     * 批量删除文件
     *
     * @param keys 文件路径列表
     */
    @Override
    public void deleteFiles(List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return;
        }

        List<ObjectIdentifier> objects = keys.stream()
                .map(k -> ObjectIdentifier.builder().key(k).build())
                .collect(Collectors.toList());

        DeleteObjectsRequest request = DeleteObjectsRequest.builder()
                .bucket(s3Properties.getBucketName())
                .delete(Delete.builder().objects(objects).build())
                .build();

        try {
            s3Client.deleteObjects(request);
            log.info("S3批量文件删除成功，bucket={}, keys={}", s3Properties.getBucketName(), keys);
        } catch (Exception e) {
            log.error("S3批量文件删除失败，bucket={}, keys={}", s3Properties.getBucketName(), keys, e);
            throw new RuntimeException("S3批量文件删除失败", e);
        }
    }

    /**
     * 递归删除指定前缀下的所有文件（模拟删除“目录”）
     *
     * @param prefix 文件名前缀，如 "folder/subfolder/"
     */
    @Override
    public void deleteFolderRecursively(String prefix) {
        String bucket = s3Properties.getBucketName();
        String continuationToken = null;

        try {
            do {
                ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                        .bucket(bucket)
                        .prefix(prefix)
                        .continuationToken(continuationToken)
                        .build();

                ListObjectsV2Response listResponse = s3Client.listObjectsV2(listRequest);
                List<S3Object> objects = listResponse.contents();

                if (objects.isEmpty()) {
                    log.info("S3路径下无文件需要删除，bucket={}, prefix={}", bucket, prefix);
                    break;
                }

                List<ObjectIdentifier> toDelete = objects.stream()
                        .map(obj -> ObjectIdentifier.builder().key(obj.key()).build())
                        .collect(Collectors.toList());

                DeleteObjectsRequest deleteRequest = DeleteObjectsRequest.builder()
                        .bucket(bucket)
                        .delete(Delete.builder().objects(toDelete).build())
                        .build();

                s3Client.deleteObjects(deleteRequest);
                log.info("已删除S3文件，bucket={}, keys={}", bucket,
                        toDelete.stream().map(ObjectIdentifier::key).collect(Collectors.toList()));

                continuationToken = listResponse.nextContinuationToken();
            } while (continuationToken != null);
        } catch (Exception e) {
            log.error("递归删除S3文件失败，bucket={}, prefix={}", bucket, prefix, e);
            throw new RuntimeException("S3递归删除失败", e);
        }
    }

    /**
     * 判断对象是否存在
     *
     * @param key 文件路径
     * @return 是否存在
     */
    @Override
    public boolean doesObjectExist(String key) {
        int notFoundStatus = 404;

        try {
            HeadObjectRequest request = HeadObjectRequest.builder()
                    .bucket(s3Properties.getBucketName())
                    .key(key)
                    .build();
            s3Client.headObject(request);
            return true;
        } catch (S3Exception e) {
            if (e.statusCode() != notFoundStatus) {
                log.warn("检查S3对象存在性时发生异常，bucket={}, key={}", s3Properties.getBucketName(), key, e);
            }
            return false;
        } catch (Exception e) {
            log.error("检查S3对象存在性失败，bucket={}, key={}", s3Properties.getBucketName(), key, e);
            return false;
        }
    }

    /**
     * 列出某个前缀（目录）下的文件
     *
     * @param prefix 文件前缀（类似文件夹路径）
     * @return 文件列表
     */
    @Override
    public List<S3Object> listFiles(String prefix) {
        try {
            ListObjectsV2Request request = ListObjectsV2Request.builder()
                    .bucket(s3Properties.getBucketName())
                    .prefix(prefix)
                    .build();

            ListObjectsV2Response response = s3Client.listObjectsV2(request);
            return response.contents();
        } catch (Exception e) {
            log.error("列出S3文件失败，bucket={}, prefix={}", s3Properties.getBucketName(), prefix, e);
            throw new RuntimeException("S3列出文件失败", e);
        }
    }

    /**
     * 列出某个前缀（目录）下的文件
     *
     * @param prefix 文件前缀（类似文件夹路径）
     * @return 文件列表
     */
    @Override
    public List<String> listFilesStr(String prefix) {
        return listFiles(prefix).stream()
                .map(S3Object::key)
                .collect(Collectors.toList());
    }

    /**
     * 生成文件的临时访问链接
     * curl示例：curl -X PUT -T myfile.jpg "https://your-presigned-url-from-java"
     *
     * @param key      文件路径（S3 Key）
     * @param duration 链接有效时长
     * @return 访问链接 URL
     */
    @Override
    public String generatePresignedUrl(String key, Duration duration) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(s3Properties.getBucketName())
                    .key(key)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(duration)
                    .getObjectRequest(getObjectRequest)
                    .build();

            URL url = s3Presigner.presignGetObject(presignRequest).url();
            return url.toString();
        } catch (Exception e) {
            log.error("生成S3预签名URL失败，bucket={}, key={}", s3Properties.getBucketName(), key, e);
            throw new RuntimeException("S3生成预签名URL失败", e);
        }
    }

    /**
     * 生成用于临时上传文件的 Presigned URL（PUT 方法）
     *
     * @param key      要上传到的 S3 路径（key）
     * @param duration 上传链接的有效时长
     * @return 上传用的临时 URL
     */
    @Override
    public String generatePresignedUploadUrl(String key, Duration duration) {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(s3Properties.getBucketName())
                    .key(key)
                    .build();

            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(duration)
                    .putObjectRequest(putObjectRequest)
                    .build();

            URL url = s3Presigner.presignPutObject(presignRequest).url();
            return url.toString();
        } catch (Exception e) {
            log.error("生成S3上传预签名URL失败，bucket={}, key={}", s3Properties.getBucketName(), key, e);
            throw new RuntimeException("S3生成上传预签名URL失败", e);
        }
    }

    /**
     * 生成公开桶文件的直链访问URL（无需签名，文件必须设置为公开读权限）
     *
     * @param key 文件路径（S3 Key）
     * @return 公开访问的完整URL
     */
    @Override
    public String generatePublicUrl(String key) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("S3对象键不能为空");
        }

        if (!doesObjectExist(key)) {
            throw new IllegalStateException("S3对象不存在，key=" + key);
        }

        String endpoint = s3Properties.getEndpoint();
        String bucket = s3Properties.getBucketName();

        String slash = "/";
        int startIndex = 0;
        int removeOne = 1;

        // 去掉 endpoint 末尾的斜杠
        if (endpoint.endsWith(slash)) {
            endpoint = endpoint.substring(startIndex, endpoint.length() - removeOne);
        }

        // 去掉 bucket 首尾的斜杠
        if (bucket.startsWith(slash)) {
            bucket = bucket.substring(startIndex + removeOne);
        }
        if (bucket.endsWith(slash)) {
            bucket = bucket.substring(startIndex, bucket.length() - removeOne);
        }

        // 去掉 key 开头的斜杠
        if (key.startsWith(slash)) {
            key = key.substring(startIndex + removeOne);
        }

        return endpoint + slash + bucket + slash + key;
    }

}
