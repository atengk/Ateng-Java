package io.github.atengk.aws.s3.model.info;

import java.time.Instant;

/**
 * S3 对象版本信息
 *
 * @param bucketName   存储桶名称
 * @param objectKey    对象 Key
 * @param versionId    版本 ID
 * @param latest       是否最新版本
 * @param deleteMarker 是否删除标记
 * @param size         对象大小
 * @param eTag         ETag
 * @param lastModified 最后修改时间
 * @author Ateng
 * @since 2026-04-28
 */
public record S3ObjectVersionInfo(
        String bucketName,
        String objectKey,
        String versionId,
        Boolean latest,
        Boolean deleteMarker,
        Long size,
        String eTag,
        Instant lastModified
) {
}
