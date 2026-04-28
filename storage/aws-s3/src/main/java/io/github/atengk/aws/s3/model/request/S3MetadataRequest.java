package io.github.atengk.aws.s3.model.request;

import io.github.atengk.aws.s3.model.enums.S3StorageClass;

import java.util.Map;

/**
 * S3 对象元数据替换请求
 *
 * @param bucketName   存储桶名称
 * @param objectKey    对象 Key
 * @param metadata     新元数据
 * @param preserveTags 是否保留标签
 * @param preserveAcl  是否保留 ACL
 * @param storageClass 存储类型
 * @author Ateng
 * @since 2026-04-28
 */
public record S3MetadataRequest(
        String bucketName,
        String objectKey,
        Map<String, String> metadata,
        Boolean preserveTags,
        Boolean preserveAcl,
        S3StorageClass storageClass
) {
}
