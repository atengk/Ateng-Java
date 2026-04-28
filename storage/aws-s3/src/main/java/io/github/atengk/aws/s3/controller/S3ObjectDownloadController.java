package io.github.atengk.aws.s3.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.aws.s3.model.request.S3DownloadRequest;
import io.github.atengk.aws.s3.model.result.S3DownloadResult;
import io.github.atengk.aws.s3.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/**
 * S3 对象下载接口
 *
 * @author Ateng
 * @since 2026-04-28
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/s3/objects/download")
public class S3ObjectDownloadController {

    private final S3Service s3Service;

    /**
     * 下载对象为字节数组
     * <p>
     * bucketName 不传时使用默认存储桶。
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/download/bytes?objectKey=upload/2026/04/28/test.txt" \
     * -o test.txt
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/download/bytes?bucketName=data&objectKey=upload/2026/04/28/test.txt" \
     * -o test.txt
     *
     * @param bucketName 存储桶名称，可为空
     * @param objectKey  对象 Key
     * @return 字节数组响应
     */
    @GetMapping("/bytes")
    public ResponseEntity<byte[]> downloadAsBytes(@RequestParam(required = false) String bucketName,
                                                  @RequestParam String objectKey) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        byte[] bytes = s3Service.downloadAsBytes(resolvedBucketName, objectKey);
        String filename = s3Service.getFilename(objectKey);
        String contentType = s3Service.getObjectContentType(resolvedBucketName, objectKey);

        log.info("下载 S3 对象为字节数组成功，bucketName={}，objectKey={}，size={}",
                resolvedBucketName, objectKey, bytes.length);

        return ResponseEntity.ok()
                .headers(buildDownloadHeaders(filename, contentType, (long) bytes.length))
                .body(bytes);
    }

    /**
     * 流式下载对象
     * <p>
     * bucketName 不传时使用默认存储桶。
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/download/stream?objectKey=video/demo.mp4" \
     * -o demo.mp4
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/download/stream?bucketName=data&objectKey=video/demo.mp4" \
     * -o demo.mp4
     *
     * @param bucketName 存储桶名称，可为空
     * @param objectKey  对象 Key
     * @return 流式下载响应
     */
    @GetMapping("/stream")
    public ResponseEntity<Resource> downloadAsStream(@RequestParam(required = false) String bucketName,
                                                     @RequestParam String objectKey) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        S3DownloadResult result = s3Service.download(new S3DownloadRequest(
                resolvedBucketName,
                objectKey,
                null,
                null,
                null
        ));

        Resource resource = buildInputStreamResource(result);

        log.info("流式下载 S3 对象成功，bucketName={}，objectKey={}，contentLength={}",
                result.bucketName(), result.objectKey(), result.contentLength());

        return ResponseEntity.ok()
                .headers(buildDownloadHeaders(result.filename(), result.contentType(), result.contentLength()))
                .body(resource);
    }

    /**
     * 下载对象为 Spring Resource
     * <p>
     * bucketName 不传时使用默认存储桶。
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/download/resource?objectKey=image/avatar.png" \
     * -o avatar.png
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/download/resource?bucketName=data&objectKey=image/avatar.png" \
     * -o avatar.png
     *
     * @param bucketName 存储桶名称，可为空
     * @param objectKey  对象 Key
     * @return Resource 下载响应
     */
    @GetMapping("/resource")
    public ResponseEntity<Resource> downloadAsResource(@RequestParam(required = false) String bucketName,
                                                       @RequestParam String objectKey) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        Resource resource = s3Service.downloadAsResource(resolvedBucketName, objectKey);
        String filename = StrUtil.blankToDefault(resource.getFilename(), s3Service.getFilename(objectKey));
        String contentType = s3Service.getObjectContentType(resolvedBucketName, objectKey);
        Long contentLength = s3Service.getObjectSize(resolvedBucketName, objectKey);

        log.info("下载 S3 对象为 Resource 成功，bucketName={}，objectKey={}，contentLength={}",
                resolvedBucketName, objectKey, contentLength);

        return ResponseEntity.ok()
                .headers(buildDownloadHeaders(filename, contentType, contentLength))
                .body(resource);
    }

    /**
     * 下载对象到服务端本地文件
     * <p>
     * 说明：targetPath 是服务端机器上的目标路径，不是客户端本机路径。
     * bucketName 不传时使用默认存储桶。
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/download/local-file?objectKey=excel/report.xlsx&targetPath=/data/download/report.xlsx"
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/download/local-file?bucketName=data&objectKey=excel/report.xlsx&targetPath=/data/download/report.xlsx"
     *
     * @param bucketName 存储桶名称，可为空
     * @param objectKey  对象 Key
     * @param targetPath 服务端本地目标路径
     * @return 下载结果
     */
    @GetMapping("/local-file")
    public Dict downloadToLocalFile(@RequestParam(required = false) String bucketName,
                                    @RequestParam String objectKey,
                                    @RequestParam String targetPath) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        Path resolvedTargetPath = Path.of(targetPath);

        s3Service.downloadToFile(resolvedBucketName, objectKey, resolvedTargetPath);

        log.info("下载 S3 对象到服务端本地文件成功，bucketName={}，objectKey={}，targetPath={}",
                resolvedBucketName, objectKey, resolvedTargetPath);

        return Dict.create()
                .set("bucketName", resolvedBucketName)
                .set("objectKey", objectKey)
                .set("targetPath", resolvedTargetPath.toString())
                .set("filename", FileUtil.getName(resolvedTargetPath.toString()))
                .set("downloaded", true);
    }

    /**
     * 高级下载对象
     * <p>
     * 支持 versionId 和 Range 下载。
     * bucketName 不传时使用默认存储桶。
     * filename 不传时使用 objectKey 中的文件名。
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/download/advanced?objectKey=video/demo.mp4&rangeStart=0&rangeEnd=1048575" \
     * -o demo.part
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/download/advanced?bucketName=data&objectKey=archive/report.pdf&versionId=your-version-id&filename=report.pdf" \
     * -o report.pdf
     *
     * @param bucketName 存储桶名称，可为空
     * @param objectKey  对象 Key
     * @param versionId  对象版本 ID，可为空
     * @param rangeStart Range 起始字节，可为空
     * @param rangeEnd   Range 结束字节，可为空
     * @param filename   下载文件名，可为空
     * @return 高级下载响应
     */
    @GetMapping("/advanced")
    public ResponseEntity<Resource> downloadAdvanced(@RequestParam(required = false) String bucketName,
                                                     @RequestParam String objectKey,
                                                     @RequestParam(required = false) String versionId,
                                                     @RequestParam(required = false) Long rangeStart,
                                                     @RequestParam(required = false) Long rangeEnd,
                                                     @RequestParam(required = false) String filename) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);

        S3DownloadRequest request = new S3DownloadRequest(
                resolvedBucketName,
                objectKey,
                blankToNull(versionId),
                rangeStart,
                rangeEnd
        );

        S3DownloadResult result = s3Service.download(request);
        Resource resource = buildInputStreamResource(result);
        String resolvedFilename = StrUtil.blankToDefault(filename, result.filename());

        HttpHeaders headers = buildDownloadHeaders(resolvedFilename, result.contentType(), result.contentLength());
        if (rangeStart != null || rangeEnd != null) {
            headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");
        }

        log.info("高级下载 S3 对象成功，bucketName={}，objectKey={}，versionId={}，rangeStart={}，rangeEnd={}，contentLength={}",
                result.bucketName(),
                result.objectKey(),
                StrUtil.blankToDefault(versionId, "默认版本"),
                rangeStart,
                rangeEnd,
                result.contentLength());

        return ResponseEntity.ok()
                .headers(headers)
                .body(resource);
    }

    /**
     * 构建输入流资源
     *
     * @param result 下载结果
     * @return 输入流资源
     */
    private Resource buildInputStreamResource(S3DownloadResult result) {
        InputStream inputStream = result.inputStream();

        return new InputStreamResource(inputStream) {

            /**
             * 获取资源文件名
             *
             * @return 文件名
             */
            @Override
            public String getFilename() {
                return result.filename();
            }

            /**
             * 获取资源内容长度
             *
             * @return 内容长度
             */
            @Override
            public long contentLength() {
                return result.contentLength() == null ? -1L : result.contentLength();
            }
        };
    }

    /**
     * 构建下载响应头
     *
     * @param filename      文件名
     * @param contentType   Content-Type
     * @param contentLength 内容长度
     * @return 响应头
     */
    private HttpHeaders buildDownloadHeaders(String filename, String contentType, Long contentLength) {
        String resolvedFilename = StrUtil.blankToDefault(filename, "download");
        MediaType mediaType = parseMediaType(contentType);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(resolvedFilename, StandardCharsets.UTF_8)
                .build());

        if (contentLength != null && contentLength >= 0) {
            headers.setContentLength(contentLength);
        }

        return headers;
    }

    /**
     * 解析 MediaType
     *
     * @param contentType Content-Type
     * @return MediaType
     */
    private MediaType parseMediaType(String contentType) {
        if (StrUtil.isBlank(contentType)) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }

        try {
            return MediaType.parseMediaType(contentType);
        } catch (IllegalArgumentException e) {
            log.warn("S3 对象 Content-Type 不合法，使用默认下载类型，contentType={}", contentType);
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    /**
     * 空白字符串转 null，并去除前后空格
     *
     * @param value 字符串
     * @return 非空白字符串或 null
     */
    private String blankToNull(String value) {
        return StrUtil.isBlank(value) ? null : StrUtil.trim(value);
    }
}