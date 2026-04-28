package io.github.atengk.aws.s3.model.result;

import java.time.Instant;

/**
 * S3 对象上传结果
 *
 * @param bucketName       存储桶名称
 * @param objectKey        对象 Key
 * @param originalFilename 原始文件名
 * @param contentType      内容类型
 * @param size             文件大小
 * @param eTag             ETag
 * @param versionId        版本 ID
 * @param url              访问 URL
 * @param uploadTime       上传时间
 * @author Ateng
 * @since 2026-04-28
 */
public record S3UploadResult(
        String bucketName,
        String objectKey,
        String originalFilename,
        String contentType,
        Long size,
        String eTag,
        String versionId,
        String url,
        Instant uploadTime
) {
}
