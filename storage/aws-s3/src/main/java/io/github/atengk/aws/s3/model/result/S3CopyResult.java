package io.github.atengk.aws.s3.model.result;

import java.time.Instant;

/**
 * S3 对象复制结果
 *
 * @param sourceBucketName 源存储桶名称
 * @param sourceKey        源对象 Key
 * @param targetBucketName 目标存储桶名称
 * @param targetKey        目标对象 Key
 * @param eTag             ETag
 * @param versionId        版本 ID
 * @param copyTime         复制时间
 * @author Ateng
 * @since 2026-04-28
 */
public record S3CopyResult(
        String sourceBucketName,
        String sourceKey,
        String targetBucketName,
        String targetKey,
        String eTag,
        String versionId,
        Instant copyTime
) {
}
