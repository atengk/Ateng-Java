# AWS S3

AWS SDK for S3 是亚马逊官方提供的开发工具包，用于与 Amazon S3（Simple Storage Service）进行交互。它支持文件上传、下载、删除、列举对象等功能，并封装了身份验证、分段上传、权限控制等操作，方便开发者在 Java、Python、Node.js 等语言中高效地集成 S3 服务。



## 基础配置

### 添加依赖

```xml
<dependencies>
    <!-- AWS SDK for S3 -->
    <dependency>
        <groupId>software.amazon.awssdk</groupId>
        <artifactId>s3</artifactId>
    </dependency>
</dependencies>
<dependencyManagement>
    <dependencies>
        <!-- AWS SDK 依赖管理 -->
        <dependency>
            <groupId>software.amazon.awssdk</groupId>
            <artifactId>bom</artifactId>
            <version>${awssdk.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### 编辑配置文件

```yaml
server:
  port: 14002
  servlet:
    context-path: /
spring:
  main:
    web-application-type: servlet
  application:
    name: ${project.artifactId}
---
# 设置文件和请求大小
spring:
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB
---
# S3 配置
s3:
  endpoint: http://192.168.1.12:20006
  access-key: admin
  secret-key: Admin@123
  region: us-east-1
  bucket-name: data
```

### 创建配置属性类

```java
package local.ateng.java.awss3.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * S3 配置属性类
 *
 * @author Ateng
 * @since 2025-07-18
 */
@Configuration
@ConfigurationProperties(prefix = "s3")
@Data
public class S3Properties {
    private String bucketName;
    private String accessKey;
    private String secretKey;
    private String region;
    private String endpoint;
}
```

### 创建配置类

```java
package local.ateng.java.awss3.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

/**
 * S3 配置类
 *
 * @author Ateng
 * @since 2025-07-18
 */
@Configuration
@RequiredArgsConstructor
public class S3Config {

    private final S3Properties s3Properties;

    /**
     * S3Client Bean
     */
    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .endpointOverride(URI.create(s3Properties.getEndpoint()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(
                                s3Properties.getAccessKey(),
                                s3Properties.getSecretKey()
                        )
                ))
                .region(Region.of(s3Properties.getRegion()))
                .build();
    }

    /**
     * S3Presigner Bean
     */
    @Bean
    public S3Presigner s3Presigner(S3Properties s3Properties) {
        return S3Presigner.builder()
                .endpointOverride(URI.create(s3Properties.getEndpoint()))
                .region(Region.of(s3Properties.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(s3Properties.getAccessKey(), s3Properties.getSecretKey())
                ))
                .build();
    }

}
```

### 创建服务类

```java
package local.ateng.java.awss3.service;

import local.ateng.java.awss3.config.S3Properties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * S3 服务类
 *
 * @author Ateng
 * @since 2025-07-18
 */
@Service
@RequiredArgsConstructor
public class S3Service {
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
    public static byte[] toByteArray(InputStream inputStream) throws IOException {
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
     * 上传文件到 S3
     *
     * <p>该方法是最通用的文件上传方式，只要求提供 S3 的 Key 和输入流。
     * 会自动尝试读取输入流为字节数组上传，适合小中型文件。</p>
     *
     * @param key         文件在 S3 中的完整路径（如：folder/test.pdf）
     * @param inputStream 输入流，来自文件、网络或内存
     * @throws RuntimeException 读取失败或上传失败时抛出
     */
    public void uploadFile(String key, InputStream inputStream) {
        try {
            // 读取输入流为字节数组
            byte[] data = toByteArray(inputStream);

            PutObjectRequest request = PutObjectRequest
                    .builder()
                    .bucket(s3Properties.getBucketName())
                    .key(key)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM_VALUE)
                    .contentLength((long) data.length).build();

            s3Client.putObject(request, RequestBody.fromBytes(data));
        } catch (IOException e) {
            throw new RuntimeException("读取上传文件失败", e);
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
    public void uploadFile(String key, InputStream inputStream, long contentLength, String contentType) {
        PutObjectRequest request = PutObjectRequest.builder().bucket(s3Properties.getBucketName()).key(key).contentType(contentType).build();

        s3Client.putObject(request, RequestBody.fromInputStream(inputStream, contentLength));
    }

    /**
     * 上传文件到 S3（通过字节数组）
     *
     * @param key         文件路径
     * @param data        文件字节内容
     * @param contentType 文件类型（如 "application/json"）
     */
    public void uploadFile(String key, byte[] data, String contentType) {
        PutObjectRequest request = PutObjectRequest.builder().bucket(s3Properties.getBucketName()).key(key).contentType(contentType).build();

        s3Client.putObject(request, RequestBody.fromBytes(data));
    }

    /**
     * 上传文件到 S3（通过本地文件 File 对象）
     *
     * @param key  目标路径（包含文件名）
     * @param file 本地文件对象
     */
    public void uploadFile(String key, File file) {
        PutObjectRequest request = PutObjectRequest.builder().bucket(s3Properties.getBucketName()).key(key).build();

        s3Client.putObject(request, RequestBody.fromFile(file));
    }

    /**
     * 上传文件到 S3（处理来自前端 Multipart 请求）
     *
     * @param key           上传目标路径（S3 中的 key）
     * @param multipartFile Spring MVC 接收到的文件对象
     * @throws RuntimeException 上传失败抛出异常
     */
    public void uploadFile(String key, MultipartFile multipartFile) {
        try {
            PutObjectRequest request = PutObjectRequest.builder().bucket(s3Properties.getBucketName()).key(key).contentType(multipartFile.getContentType()).build();

            s3Client.putObject(request, RequestBody.fromInputStream(multipartFile.getInputStream(), multipartFile.getSize()));
        } catch (IOException e) {
            throw new RuntimeException("上传失败: " + e.getMessage(), e);
        }
    }

    /**
     * 上传多个文件到 S3（处理来自前端 Multipart 请求）
     *
     * @param keys           上传目标路径集合（S3 中的多个 key）
     * @param multipartFiles Spring MVC 接收到的文件对象集合
     * @throws RuntimeException 上传失败抛出异常
     */
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
     * @param keys        上传目标路径集合（S3 中的多个 key）
     * @param inputStreams 输入流集合（每个流代表一个文件）
     * @throws RuntimeException 上传失败时抛出异常
     */
    public void uploadMultipleFilesWithStreams(List<String> keys, List<InputStream> inputStreams) {
        if (keys.size() != inputStreams.size()) {
            throw new IllegalArgumentException("上传的文件路径和文件数量不匹配！");
        }

        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            InputStream inputStream = inputStreams.get(i);
            uploadFile(key, inputStream);  // 假设 uploadFile 支持 InputStream 上传
        }
    }

    /**
     * 并发上传多个文件到 S3（处理来自前端的 InputStream 文件）
     * 使用默认线程池（ForkJoinPool.commonPool）
     *
     * @param keys          上传目标路径集合（S3 中的多个 key）
     * @param inputStreams  输入流集合（每个流代表一个文件）
     * @throws RuntimeException 上传失败时抛出异常
     */
    public void uploadMultipleFilesAsyncWithStreams(List<String> keys, List<InputStream> inputStreams) {
        uploadMultipleFilesAsyncWithStreams(keys, inputStreams, false);
    }

    /**
     * 并发上传多个文件到 S3（处理来自前端的 InputStream 文件）
     * 支持忽略单个上传错误（通过 ignoreErrors 参数控制）
     * 使用默认线程池（ForkJoinPool.commonPool）
     *
     * @param keys          上传目标路径集合（S3 中的多个 key）
     * @param inputStreams  输入流集合（每个流代表一个文件）
     * @param ignoreErrors  是否忽略单个文件上传错误，true 表示继续上传其他文件
     */
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
    public ResponseInputStream<GetObjectResponse> downloadFile(String key) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(s3Properties.getBucketName())
                .key(key)
                .build();

        return s3Client.getObject(request);
    }

    /**
     * 下载文件并写入响应流，用于浏览器下载
     *
     * @param key      S3 文件路径
     * @param fileName 下载时的文件名
     * @param response HttpServletResponse
     */
    public void downloadToResponse(String key, String fileName, HttpServletResponse response) {
        try (ResponseInputStream<GetObjectResponse> s3Stream = downloadFile(key);
             OutputStream out = response.getOutputStream()) {

            GetObjectResponse objectResponse = s3Stream.response();

            response.setContentType(objectResponse.contentType() != null ? objectResponse.contentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE);
            response.setHeader("Content-Disposition", "attachment; filename=\"" + URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString()) + "\"");
            response.setHeader("Content-Length", String.valueOf(objectResponse.contentLength()));

            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = s3Stream.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }

            out.flush();
        } catch (IOException e) {
            throw new RuntimeException("文件下载失败: " + key, e);
        }
    }

    /**
     * 下载文件并保存到本地路径
     *
     * @param key       S3 路径
     * @param localPath 本地保存路径
     */
    public void downloadToFile(String key, Path localPath) {
        try (ResponseInputStream<GetObjectResponse> s3Stream = downloadFile(key)) {
            // 确保父目录存在
            Path parentDir = localPath.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }

            // 写入文件
            Files.copy(s3Stream, localPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("下载文件失败并保存到本地: " + key, e);
        }
    }

    /**
     * 批量下载多个文件并保存到本地路径
     *
     * @param keys         S3 文件路径列表
     * @param localPaths   本地保存路径列表
     * @param ignoreErrors 是否忽略下载失败；true 表示忽略，false 表示遇到失败立即抛异常
     */
    public void downloadMultipleToFiles(List<String> keys, List<Path> localPaths, boolean ignoreErrors) {
        if (keys.size() != localPaths.size()) {
            throw new IllegalArgumentException("S3 路径数量和本地路径数量不一致！");
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
                        } catch (IOException ignored) {
                        }
                    }
                    throw new RuntimeException("下载文件失败：" + key, e);
                } else {
                    System.err.println("下载失败（已忽略）：" + key + "，原因：" + e.getMessage());
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
    public void downloadMultipleToFilesAsync(List<String> keys, List<Path> localPaths, boolean ignoreErrors) {
        if (keys.size() != localPaths.size()) {
            throw new IllegalArgumentException("S3 路径数量和本地路径数量不一致！");
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
                        System.err.println("下载失败（已忽略）: " + key + " -> " + path + ", 错误: " + e.getMessage());
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
                } catch (IOException ignored) {
                }
            }
            throw new RuntimeException("批量文件下载失败，已清理已下载的文件。", e);
        }
    }

    /**
     * 批量下载多个 S3 文件为输入流列表
     *
     * @param keys         S3 文件路径列表
     * @param ignoreErrors 是否忽略下载失败的文件；true 表示忽略，false 表示遇到失败立即抛异常
     * @return 成功下载的输入流列表（顺序与成功的 key 保持一致）
     */
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
                        } catch (IOException ignored) {
                        }
                    }
                    throw new RuntimeException("下载文件失败：" + key, e);
                } else {
                    // 如果忽略错误，则打印错误
                    System.err.println("下载失败: " + key + ", 错误: " + e.getMessage());
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
    public List<InputStream> downloadMultipleToStreamsAsync(List<String> keys, boolean ignoreErrors) {
        List<CompletableFuture<InputStream>> futures = new ArrayList<>();

        // 提交异步任务
        for (String key : keys) {
            CompletableFuture<InputStream> future = CompletableFuture.supplyAsync(() -> {
                try {
                    return downloadFile(key);  // 假设 downloadFile 方法返回文件的 InputStream
                } catch (Exception e) {
                    if (!ignoreErrors) {
                        // 如果不忽略错误，抛出异常以终止任务
                        throw new RuntimeException("下载文件失败：" + key, e);
                    } else {
                        System.err.println("下载失败（已忽略）: " + key + ", 错误: " + e.getMessage());
                        return null;  // 返回 null 表示下载失败，忽略该文件
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
                    throw new RuntimeException("任务执行失败", e.getCause());
                } else {
                    // 如果忽略错误，则打印错误
                    System.err.println("任务执行失败（已忽略）: " + e.getCause().getMessage());
                }
            }
        }

        return inputStreams;
    }

    /**
     * 批量下载多个 S3 文件并返回对应的输入流列表（默认不忽略错误）
     *
     * @param keys S3 文件路径列表
     * @return 对应文件内容的输入流列表（与 keys 一一对应）
     * @throws RuntimeException 任一文件下载失败将抛出异常
     */
    public List<InputStream> downloadMultipleToStreamsAsync(List<String> keys) {
        return downloadMultipleToStreamsAsync(keys, false);
    }

    /**
     * 删除单个文件
     *
     * @param key 文件路径
     */
    public void deleteFile(String key) {
        DeleteObjectRequest request = DeleteObjectRequest.builder().bucket(s3Properties.getBucketName()).key(key).build();

        s3Client.deleteObject(request);
    }

    /**
     * 批量删除文件
     *
     * @param keys 文件路径列表
     */
    public void deleteFiles(List<String> keys) {
        if (keys == null || keys.isEmpty()) return;

        List<ObjectIdentifier> objects = keys.stream().map(k -> ObjectIdentifier.builder().key(k).build()).collect(Collectors.toList());

        DeleteObjectsRequest request = DeleteObjectsRequest.builder().bucket(s3Properties.getBucketName()).delete(Delete.builder().objects(objects).build()).build();

        s3Client.deleteObjects(request);
    }

    /**
     * 递归删除指定前缀下的所有文件（模拟删除“目录”）
     *
     * @param prefix 文件名前缀，如 "folder/subfolder/"
     */
    public void deleteFolderRecursively(String prefix) {
        String bucket = s3Properties.getBucketName();

        String continuationToken = null;

        do {
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(bucket)
                    .prefix(prefix)
                    .continuationToken(continuationToken)
                    .build();

            ListObjectsV2Response listResponse = s3Client.listObjectsV2(listRequest);

            List<S3Object> objects = listResponse.contents();

            if (objects.isEmpty()) {
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

            continuationToken = listResponse.nextContinuationToken();

        } while (continuationToken != null);
    }

    /**
     * 判断对象是否存在
     *
     * @param key 文件路径
     * @return 是否存在
     */
    public boolean doesObjectExist(String key) {
        try {
            HeadObjectRequest request = HeadObjectRequest.builder().bucket(s3Properties.getBucketName()).key(key).build();
            s3Client.headObject(request);
            return true;
        } catch (S3Exception e) {
            return false;
        }
    }

    /**
     * 列出某个前缀（目录）下的文件
     *
     * @param prefix 文件前缀（类似文件夹路径）
     * @return 文件列表
     */
    public List<S3Object> listFiles(String prefix) {
        ListObjectsV2Request request = ListObjectsV2Request.builder().bucket(s3Properties.getBucketName()).prefix(prefix).build();

        ListObjectsV2Response response = s3Client.listObjectsV2(request);
        return response.contents();
    }

    /**
     * 生成文件的临时访问链接
     *
     * @param key      文件路径（S3 Key）
     * @param duration 链接有效时长
     * @return 访问链接 URL
     */
    public String generatePresignedUrl(String key, Duration duration) {
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
    }

    /**
     * 生成公开桶文件的直链访问URL（无需签名，文件必须设置为公开读权限）
     *
     * @param key 文件路径（S3 Key）
     * @return 公开访问的完整URL
     */
    public String generatePublicUrl(String key) {
        String endpoint = s3Properties.getEndpoint();
        String bucket = s3Properties.getBucketName();

        // 简单拼接，先去掉末尾和开头的斜杠，最后统一拼接
        endpoint = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
        bucket = bucket.startsWith("/") ? bucket.substring(1) : bucket;
        bucket = bucket.endsWith("/") ? bucket.substring(0, bucket.length() - 1) : bucket;
        key = key.startsWith("/") ? key.substring(1) : key;

        return endpoint + "/" + bucket + "/" + key;
    }


}
```



## 使用S3

### 创建接口

```java
package local.ateng.java.awss3.controller;

import local.ateng.java.awss3.service.S3Service;
import local.ateng.java.awss3.utils.ZipUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.model.S3Object;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/s3")
@RequiredArgsConstructor
public class S3Controller {

    private final S3Service s3Service;

    @PostMapping("/uploadFile")
    public ResponseEntity<Void> uploadFile(MultipartFile file, String key) {
        s3Service.uploadFile(key, file);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/uploadMultipleFiles")
    public ResponseEntity<Void> uploadMultipleFiles(String[] keys, MultipartFile[] files) {
        s3Service.uploadMultipleFiles(Arrays.asList(keys), Arrays.asList(files));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/downloadToFile")
    public ResponseEntity<Void> downloadToFile(String key, String localPath) {
        s3Service.downloadToFile(key, Paths.get(localPath));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/downloadMultipleToFilesAsync")
    public ResponseEntity<Void> downloadMultipleToFilesAsync() {
        List<String> keys = Arrays.asList("upload/1.jpg", "upload/2.jpg", "upload/3.jpg");
        List<Path> localPaths = Arrays.asList(Paths.get("D:\\temp\\download\\1.jpg"), Paths.get("D:\\temp\\download\\2.jpg"), Paths.get("D:\\temp\\download\\3.jpg"));
        s3Service.downloadMultipleToFilesAsync(keys, localPaths, true);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/listFiles")
    public ResponseEntity<List<S3Object>> listFiles() {
        List<S3Object> files = s3Service.listFiles("/");
        return ResponseEntity.ok(files);
    }

    @GetMapping("/generatePublicUrl")
    public ResponseEntity<String> generatePublicUrl(String key) {
        String url = s3Service.generatePublicUrl(key);
        return ResponseEntity.ok(url);
    }

    @GetMapping("/generatePresignedUrl")
    public ResponseEntity<String> generatePresignedUrl(String key) {
        String url = s3Service.generatePresignedUrl(key, Duration.ofHours(1));
        return ResponseEntity.ok(url);
    }

    @DeleteMapping("/deleteFile")
    public ResponseEntity<Void> deleteFile(String key) {
        s3Service.deleteFile(key);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/deleteFolderRecursively")
    public ResponseEntity<Void> deleteFolderRecursively(String key) {
        s3Service.deleteFolderRecursively(key);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/zip")
    public ResponseEntity<Void> zip(HttpServletResponse response) throws IOException {
        List<Path> localPaths = Arrays.asList(Paths.get("D:\\temp\\download\\1.jpg"), Paths.get("D:\\temp\\download\\2.jpg"));
        ZipUtil.writeZipToResponse(localPaths, response, "孔余  asdhasiu 8738&@!*&#(!.zip");
        return ResponseEntity.noContent().build();
    }

}

```

