package io.github.atengk.aws.s3.model.info;

import io.github.atengk.aws.s3.model.enums.S3StorageClass;

import java.time.Instant;
import java.util.Map;

/**
 * S3 对象信息
 *
 * @param bucketName   存储桶名称
 * @param objectKey    对象 Key
 * @param filename     文件名
 * @param contentType  内容类型
 * @param size         对象大小
 * @param eTag         ETag
 * @param versionId    版本 ID
 * @param storageClass 存储类型
 * @param lastModified 最后修改时间
 * @param metadata     自定义元数据
 * @param tags         对象标签
 * @author Ateng
 * @since 2026-04-28
 */
public record S3ObjectInfo(
        String bucketName,
        String objectKey,
        String filename,
        String contentType,
        Long size,
        String eTag,
        String versionId,
        S3StorageClass storageClass,
        Instant lastModified,
        Map<String, String> metadata,
        Map<String, String> tags
) {
}
