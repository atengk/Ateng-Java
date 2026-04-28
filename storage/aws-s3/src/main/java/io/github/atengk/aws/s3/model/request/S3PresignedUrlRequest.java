package io.github.atengk.aws.s3.model.request;

import io.github.atengk.aws.s3.model.enums.S3PresignedUrlMethod;

import java.time.Duration;
import java.util.Map;

/**
 * S3 预签名 URL 请求
 *
 * @param bucketName      存储桶名称
 * @param objectKey       对象 Key
 * @param method          请求方法
 * @param expire          有效期
 * @param contentType     内容类型
 * @param filename        下载文件名
 * @param requestHeaders  参与签名的请求头
 * @param responseHeaders GET 响应头覆盖
 * @author Ateng
 * @since 2026-04-28
 */
public record S3PresignedUrlRequest(
        String bucketName,
        String objectKey,
        S3PresignedUrlMethod method,
        Duration expire,
        String contentType,
        String filename,
        Map<String, String> requestHeaders,
        Map<String, String> responseHeaders
) {
}
